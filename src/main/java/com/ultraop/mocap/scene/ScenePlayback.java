package com.ultraop.mocap.scene;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlaybackSession;
import com.ultraop.mocap.playback.PositionTransformer;
import com.ultraop.mocap.recording.PlayerStateFrame;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Runtime scene playback tree with parent-to-child transformation chaining. */
public final class ScenePlayback {
    private final UUID id = UUID.randomUUID();
    private final SceneManager sceneManager;
    private final PlaybackManager playbackManager;
    private final Player viewer;
    private final String sceneName;
    private final boolean root;
    private final PlaybackModifiers modifiers;
    private final PositionTransformer transformer;
    private final SceneData data;
    private final List<String> ancestry;
    private final List<PlaybackSession> recordings = new ArrayList<>();
    private final List<ScenePlayback> children = new ArrayList<>();
    private boolean stopped;
    private boolean finished;
    private int waitTicks;

    private ScenePlayback(SceneManager sceneManager, PlaybackManager playbackManager, Player viewer, String sceneName,
                          boolean root, PlaybackModifiers modifiers, PositionTransformer transformer,
                          SceneData data, List<String> ancestry) {
        this.sceneManager = sceneManager;
        this.playbackManager = playbackManager;
        this.viewer = viewer;
        this.sceneName = sceneName;
        this.root = root;
        this.modifiers = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
        this.transformer = transformer;
        this.data = data;
        this.ancestry = List.copyOf(ancestry);
        this.waitTicks = secondsToTicks(this.modifiers.startDelaySeconds() + this.modifiers.waitOnStartSeconds());
    }

    public static ScenePlayback start(SceneManager sceneManager, PlaybackManager playbackManager, String sceneName,
                                      Player viewer, PlaybackModifiers modifiers) {
        return start(sceneManager, playbackManager, sceneName, viewer, modifiers, null, true, new ArrayList<>());
    }

    static ScenePlayback start(SceneManager sceneManager, PlaybackManager playbackManager, String sceneName, Player viewer,
                               PlaybackModifiers modifiers, PositionTransformer parentTransformer, boolean isRoot,
                               List<String> ancestry) {
        try {
            if (ancestry.contains(sceneName)) return null;
            SceneData data = sceneManager.load(sceneName);
            if (data == null || (isRoot && data.elements().isEmpty())) return null;
            PlaybackModifiers effective = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
            PositionTransformer transformer = createTransformer(data, effective, parentTransformer, sceneManager);
            ScenePlayback playback = new ScenePlayback(sceneManager, playbackManager, viewer, sceneName, isRoot,
                    effective, transformer, data, appendAncestry(ancestry, sceneName));
            return playback.build(playback.ancestry) ? playback : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> appendAncestry(List<String> ancestry, String sceneName) {
        List<String> result = new ArrayList<>(ancestry);
        result.add(sceneName);
        return result;
    }

    private boolean build(List<String> currentAncestry) {
        for (SceneElement element : data.elements()) {
            String name = element.name();
            PlaybackModifiers merged = element.modifiers().mergeWithParent(modifiers);
            if (name.startsWith(".")) {
                String childName = name.substring(1);
                if (childName.isBlank() || currentAncestry.contains(childName)) return false;
                ScenePlayback child = start(sceneManager, playbackManager, childName, viewer, merged, transformer,
                        false, currentAncestry);
                if (child == null) return false;
                children.add(child);
            } else {
                RecordingSession recording = sceneManager.resolveRecording(element);
                if (recording == null) return false;
                PlaybackSession session = playbackManager.createSubscene(recording, viewer, merged, transformer);
                if (session == null) return false;
                recordings.add(session);
            }
        }
        return true;
    }

    private static PositionTransformer createTransformer(SceneData data, PlaybackModifiers modifiers,
                                                         PositionTransformer parent, SceneManager manager) {
        if (parent != null && isTransformationDefault(modifiers)) return parent;
        PlaybackModifiers.TransformationConfig config = modifiers.transformationConfig();
        Vector center = null;
        if (config.sceneCenterType() != PlaybackModifiers.SceneCenterType.INDIVIDUAL && !data.elements().isEmpty()) {
            center = getSceneStartPos(data, config.sceneCenterType(), config.sceneCenterSpecific(), manager, new HashSet<>());
        }
        return new PositionTransformer(modifiers, parent, center);
    }

    private static boolean isTransformationDefault(PlaybackModifiers modifiers) {
        return modifiers.rotationDegrees() == 0.0
                && modifiers.mirror() == PlaybackModifiers.Mirror.NONE
                && modifiers.sceneScale() == 1.0
                && modifiers.offsetX() == 0.0
                && modifiers.offsetY() == 0.0
                && modifiers.offsetZ() == 0.0
                && modifiers.transformationConfig().isDefault();
    }

    private static Vector getSceneStartPos(SceneData sceneData, PlaybackModifiers.SceneCenterType type,
                                           String specific, SceneManager manager, Set<String> ancestry) {
        if (sceneData == null || sceneData.elements().isEmpty()) return null;
        SceneElement element = switch (type) {
            case COMMON_LAST -> sceneData.elements().get(sceneData.elements().size() - 1);
            case COMMON_SPECIFIC -> resolveSpecific(sceneData, specific);
            case COMMON_FIRST, INDIVIDUAL -> sceneData.elements().get(0);
        };
        return element == null ? null : getElementStartPos(element, manager, ancestry);
    }

    private static SceneElement resolveSpecific(SceneData data, String specific) {
        if (specific == null || specific.isBlank()) return data.elements().get(0);
        int dash = specific.indexOf('-');
        String position = dash >= 0 ? specific.substring(0, dash) : specific;
        String expectedName = dash >= 0 ? specific.substring(dash + 1) : null;
        try {
            int index = Integer.parseInt(position);
            if (index < 1 || index > data.elements().size()) return null;
            SceneElement element = data.elements().get(index - 1);
            return expectedName == null || expectedName.equals(element.name()) ? element : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Vector getElementStartPos(SceneElement element, SceneManager manager, Set<String> ancestry) {
        RecordingSession recording = manager.resolveRecording(element);
        if (recording != null) return calculateCenter(recordingStart(recording), element.modifiers());
        String name = element.name();
        if (!name.startsWith(".")) return null;
        String childName = name.substring(1);
        if (childName.isBlank() || !ancestry.add(childName)) return null;
        try {
            SceneData child = manager.load(childName);
            if (child == null || child.elements().isEmpty()) return null;
            PlaybackModifiers.TransformationConfig childConfig = element.modifiers().transformationConfig();
            Vector nested = getSceneStartPos(child, childConfig.sceneCenterType(), childConfig.sceneCenterSpecific(), manager, ancestry);
            return nested == null ? null : calculateCenter(nested, element.modifiers());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Vector recordingStart(RecordingSession recording) {
        PlayerStateFrame frame = recording.getFrames().isEmpty() ? null : recording.getFrames().get(0);
        return frame == null ? null : new Vector(frame.x(), frame.y(), frame.z());
    }

    private static Vector calculateCenter(Vector startPos, PlaybackModifiers modifiers) {
        if (startPos == null) return null;
        PlaybackModifiers.TransformationConfig config = modifiers.transformationConfig();
        Vector center = switch (config.recordingCenter()) {
            case ACTUAL -> startPos.clone();
            case BLOCK_CENTER -> blockCenter(startPos);
            case BLOCK_CORNER -> blockCorner(startPos);
            case AUTO -> autoCenter(startPos, modifiers.sceneScale());
        };
        return center.add(new Vector(config.centerOffsetX(), config.centerOffsetY(), config.centerOffsetZ()));
    }

    private static Vector autoCenter(Vector pos, double sceneScale) {
        if (sceneScale == 1.0 || sceneScale != Math.rint(sceneScale)) {
            Vector center = blockCenter(pos);
            Vector corner = blockCorner(pos);
            return pos.distanceSquared(center) > pos.distanceSquared(corner) ? corner : center;
        }
        return ((int) sceneScale % 2 == 1) ? blockCenter(pos) : blockCorner(pos);
    }

    private static Vector blockCenter(Vector pos) {
        return new Vector(Math.round(pos.getX() - 0.5) + 0.5, Math.floor(pos.getY()), Math.round(pos.getZ() - 0.5) + 0.5);
    }

    private static Vector blockCorner(Vector pos) {
        return new Vector(Math.round(pos.getX()), Math.floor(pos.getY()), Math.round(pos.getZ()));
    }

    public void tick() {
        if (stopped) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (finished) {
            if (modifiers.loop()) restart();
            else if (root || !modifiers.waitForParentEnd()) stop();
            return;
        }

        boolean inactive = true;
        boolean allStopped = true;
        for (PlaybackSession session : new ArrayList<>(recordings)) {
            session.advance();
            if (session.isActive()) inactive = false;
            if (!session.isStopped()) allStopped = false;
        }
        for (ScenePlayback child : new ArrayList<>(children)) {
            child.tick();
            if (child.isActive()) inactive = false;
            if (!child.isStopped()) allStopped = false;
        }

        if (inactive) finishOrWaitOnEnd();
        if (allStopped && (!finished || root || !modifiers.waitForParentEnd())) stop();
    }

    private void finishOrWaitOnEnd() {
        if (finished) return;
        int endWait = secondsToTicks(modifiers.waitOnEndSeconds());
        if (endWait > 0) {
            waitTicks = endWait;
            finished = true;
            return;
        }
        finished = true;
        if (modifiers.loop()) {
            restart();
        } else if (root || !modifiers.waitForParentEnd()) {
            stop();
        }
    }

    private void restart() {
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
        recordings.clear();
        children.clear();
        finished = false;
        stopped = false;
        waitTicks = 0;
        build(ancestry);
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        finished = true;
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
    }

    private static int secondsToTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0.0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(seconds * 20.0));
    }

    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return finished; }
    public boolean isActive() { return !stopped && (!finished || modifiers.loop() || !modifiers.waitForParentEnd()); }
    public UUID getId() { return id; }
}
