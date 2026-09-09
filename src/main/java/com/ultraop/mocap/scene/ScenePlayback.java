package com.ultraop.mocap.scene;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlaybackSession;
import com.ultraop.mocap.playback.PositionTransformer;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Runtime scene playback tree with parent-to-child transformation chaining. */
public final class ScenePlayback {
    private final UUID id = UUID.randomUUID();
    private final SceneManager sceneManager;
    private final PlaybackManager playbackManager;
    private final Player viewer;
    private final String sceneName;
    private final PlaybackModifiers modifiers;
    private final PositionTransformer transformer;
    private final SceneData data;
    private final List<PlaybackSession> recordings = new ArrayList<>();
    private final List<ScenePlayback> children = new ArrayList<>();
    private boolean stopped;
    private boolean finished;
    private int waitTicks;

    private ScenePlayback(SceneManager sceneManager, PlaybackManager playbackManager, Player viewer, String sceneName,
                          PlaybackModifiers modifiers, PositionTransformer transformer, SceneData data) {
        this.sceneManager = sceneManager;
        this.playbackManager = playbackManager;
        this.viewer = viewer;
        this.sceneName = sceneName;
        this.modifiers = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
        this.transformer = transformer;
        this.data = data;
        this.waitTicks = secondsToTicks(this.modifiers.startDelaySeconds() + this.modifiers.waitOnStartSeconds());
    }

    public static ScenePlayback start(SceneManager sceneManager, PlaybackManager playbackManager,
                                      String sceneName, Player viewer, PlaybackModifiers modifiers) {
        return start(sceneManager, playbackManager, sceneName, viewer, modifiers, null, true, new ArrayList<>());
    }

    static ScenePlayback start(SceneManager sceneManager, PlaybackManager playbackManager, String sceneName,
                               Player viewer, PlaybackModifiers modifiers, PositionTransformer parentTransformer,
                               boolean isRoot, List<String> ancestry) {
        try {
            SceneData data = sceneManager.load(sceneName);
            if (data == null || (isRoot && data.elements().isEmpty())) return null;
            PlaybackModifiers effective = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
            PositionTransformer transformer = createTransformer(data, effective, parentTransformer, sceneManager);
            ScenePlayback playback = new ScenePlayback(sceneManager, playbackManager, viewer, sceneName, effective, transformer, data);
            if (!playback.build(ancestry)) return null;
            return playback;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean build(List<String> ancestry) {
        for (SceneElement element : data.elements()) {
            String name = element.name();
            PlaybackModifiers merged = element.modifiers().mergeWithParent(modifiers);
            if (name.startsWith(".")) {
                String childName = name.substring(1);
                if (childName.isBlank() || ancestry.contains(childName)) return false;
                List<String> next = new ArrayList<>(ancestry);
                next.add(childName);
                ScenePlayback child = start(sceneManager, playbackManager, childName, viewer, merged,
                        transformer, false, next);
                if (child == null) return false;
                children.add(child);
            } else {
                RecordingSession recording = sceneManager.resolveRecording(element);
                if (recording == null) return false;
                PlaybackSession session = playbackManager.create(recording, viewer, merged, transformer);
                if (session == null) return false;
                recordings.add(session);
            }
        }
        return true;
    }

    private static PositionTransformer createTransformer(SceneData data, PlaybackModifiers modifiers,
                                                         PositionTransformer parent, SceneManager manager) {
        if (parent != null && modifiers == PlaybackModifiers.DEFAULT) return parent;
        Vector center = null;
        if (!data.elements().isEmpty()) {
            SceneElement first = data.elements().get(0);
            RecordingSession recording = manager.resolveRecording(first);
            if (recording != null && !recording.getFrames().isEmpty()) {
                var frame = recording.getFrames().get(0);
                center = new Vector(frame.x(), frame.y(), frame.z());
            }
        }
        return new PositionTransformer(modifiers, parent, center);
    }

    public void tick() {
        if (stopped) return;
        if (waitTicks > 0) { waitTicks--; return; }

        boolean inactive = true;
        boolean allStopped = true;
        for (PlaybackSession session : new ArrayList<>(recordings)) {
            session.advance();
            if (!session.isFinished()) inactive = false;
            if (!session.isStopped()) allStopped = false;
        }
        for (ScenePlayback child : new ArrayList<>(children)) {
            child.tick();
            if (!child.isFinished()) inactive = false;
            if (!child.isStopped()) allStopped = false;
        }

        if (inactive) finishOrWaitOnEnd();
        if (allStopped && (!recordings.isEmpty() || !children.isEmpty())) stop();
    }

    private void finishOrWaitOnEnd() {
        if (finished) return;
        finished = true;
        if (modifiers.loop()) {
            restart();
            return;
        }
        int endWait = secondsToTicks(modifiers.waitOnEndSeconds());
        if (endWait > 0) {
            waitTicks = endWait;
        } else if (modifiers.waitForParentEnd() || sceneName != null) {
            stop();
        }
    }

    private void restart() {
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
        recordings.clear();
        children.clear();
        finished = false;
        waitTicks = secondsToTicks(modifiers.waitOnStartSeconds());
        build(List.of(sceneName));
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        finished = true;
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
    }

    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return finished; }
    public UUID getId() { return id; }
}
