package com.ultraop.mocap.playback;

/** Per-owner playback modifiers, matching the command-facing MoCap modifier model. */
public record PlaybackModifiers(
        double startDelaySeconds,
        double waitOnStartSeconds,
        double waitOnEndSeconds,
        boolean waitForParentEnd,
        boolean loop,
        double rotationDegrees,
        Mirror mirror,
        double playerScale,
        double sceneScale,
        double offsetX,
        double offsetY,
        double offsetZ) {

    public enum Mirror { NONE, X, Z, XZ }

    public static final PlaybackModifiers DEFAULT = new PlaybackModifiers(
            0.0, 0.0, 0.0, false, false, 0.0, Mirror.NONE,
            1.0, 1.0, 0.0, 0.0, 0.0);

    public PlaybackModifiers withStartDelay(double value) { return new PlaybackModifiers(value, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitOnStart(double value) { return new PlaybackModifiers(startDelaySeconds, value, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitOnEnd(double value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, value, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitForParentEnd(boolean value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, value, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withLoop(boolean value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, value, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withRotation(double value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, value, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withMirror(Mirror value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, value, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withPlayerScale(double value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, value, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withSceneScale(double value) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, value, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withOffset(double x, double y, double z) { return new PlaybackModifiers(startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, x, y, z); }
}
