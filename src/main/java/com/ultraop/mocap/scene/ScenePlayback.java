package com.ultraop.mocap.scene;

import com.ultraop.mocap.playback.PlaybackManager;
import com.ultraop.mocap.playback.PlaybackModifiers;
import com.ultraop.mocap.playback.PlaybackSession;
import com.ultraop.mocap.recording.RecordingSession;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Runtime scene playback tree. Mirrors the upstream parent/child lifecycle model. */
public final class ScenePlayback {
    private final UUID id = UUID.randomUUID();
    private final SceneManager sceneManager;
    private final PlaybackManager playbackManager;
    private final Player viewer;
    private final PlaybackModifiers modifiers;
    private final List<PlaybackSession> recordings = new ArrayList<>();
    private final List<ScenePlayback> children = new ArrayList<>();
    private boolean stopped;
    private boolean finished;
    private int waitTicks;

    private ScenePlayback(SceneManager sceneManager, PlaybackManager playbackManager, Player viewer,
                          PlaybackModifiers modifiers) {
        this.sceneManager = sceneManager;
        this.playbackManager = playbackManager;
        this.viewer = viewer;
        this.modifiers = modifiers;
        this.waitTicks = secondsToTicks(modifiers.startDelaySeconds() + modifiers.waitOnStartSeconds());
    }

    public static ScenePlayback start(SceneManager sceneManager, PlaybackManager playbackManager,
                                      String sceneName, Player viewer, PlaybackModifiers modifiers) {
        try {
            SceneData data = sceneManager.load(sceneName);
            if (data == null || data.elements().isEmpty()) return null;
            ScenePlayback playback = new ScenePlayback(sceneManager, playbackManager, viewer, modifiers);
            if (!playback.build(data, new ArrayList<>())) return null;
            return playback;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean build(SceneData data, List<String> ancestry) {
        for (SceneElement element : data.elements()) {
            String name = element.name();
            PlaybackModifiers merged = merge(modifiers, element.modifiers());
            if (name.startsWith(".")) {
                String childName = name.substring(1);
                if (childName.isBlank() || ancestry.contains(childName)) return false;
                try {
                    SceneData childData = sceneManager.load(childName);
                    if (childData == null) return false;
                    List<String> next = new ArrayList<>(ancestry);
                    next.add(childName);
                    ScenePlayback child = new ScenePlayback(sceneManager, playbackManager, viewer, merged);
                    if (!child.build(childData, next)) return false;
                    children.add(child);
                } catch (Exception e) {
                    return false;
                }
            } else {
                RecordingSession recording = sceneManager.resolveRecording(element);
                if (recording == null) return false;
                PlaybackSession session = playbackManager.play(recording, viewer, merged);
                if (session == null) return false;
                recordings.add(session);
            }
        }
        return true;
    }

    private static PlaybackModifiers merge(PlaybackModifiers parent, PlaybackModifiers child) {
        // Scene element values are already materialized; preserve the child as the effective value.
        // Runtime parent waiting is handled by this tree rather than by the standalone playback session.
        return child;
    }

    public void tick() {
        if (stopped) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        boolean active = false;
        for (PlaybackSession session : new ArrayList<>(recordings)) {
            if (!session.isStopped()) active = true;
        }
        for (ScenePlayback child : children) {
            child.tick();
            if (!child.isStopped()) active = true;
        }
        if (!active) {
            if (modifiers.loop()) {
                restart();
            } else {
                finished = true;
                if (secondsToTicks(modifiers.waitOnEndSeconds()) > 0) {
                    waitTicks = secondsToTicks(modifiers.waitOnEndSeconds());
                } else {
                    stop();
                }
            }
        }
    }

    private void restart() {
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
        recordings.clear();
        children.clear();
        finished = false;
        waitTicks = secondsToTicks(modifiers.waitOnStartSeconds());
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        for (PlaybackSession session : recordings) session.stop();
        for (ScenePlayback child : children) child.stop();
    }

    public boolean isStopped() { return stopped; }
    public boolean isFinished() { return finished; }
    public UUID getId() { return id; }

    private static int secondsToTicks(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(seconds * 20.0));
    }
}
