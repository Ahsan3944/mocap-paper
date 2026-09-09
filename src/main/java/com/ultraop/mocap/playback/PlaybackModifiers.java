package com.ultraop.mocap.playback;

/** Per-owner playback modifiers matching the command-facing MoCap model. */
public record PlaybackModifiers(
        String playerName,
        String playerSkin,
        boolean playerAsEntity,
        String entityFilter,
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
            null, null, false, null,
            0.0, 0.0, 0.0, false, false, 0.0, Mirror.NONE,
            1.0, 1.0, 0.0, 0.0, 0.0);

    public PlaybackModifiers withPlayerName(String value) { return copy(value, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withPlayerSkin(String value) { return copy(playerName, value, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withPlayerAsEntity(boolean value) { return copy(playerName, playerSkin, value, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withEntityFilter(String value) { return copy(playerName, playerSkin, playerAsEntity, value, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withStartDelay(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, value, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitOnStart(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, value, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitOnEnd(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, value, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withWaitForParentEnd(boolean value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, value, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withLoop(boolean value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, value, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withRotation(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, value, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withMirror(Mirror value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, value, playerScale, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withPlayerScale(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, value, sceneScale, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withSceneScale(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, value, offsetX, offsetY, offsetZ); }
    public PlaybackModifiers withOffset(double x, double y, double z) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, x, y, z); }

    private static PlaybackModifiers copy(String playerName, String playerSkin, boolean playerAsEntity, String entityFilter,
                                          double startDelaySeconds, double waitOnStartSeconds, double waitOnEndSeconds,
                                          boolean waitForParentEnd, boolean loop, double rotationDegrees, Mirror mirror,
                                          double playerScale, double sceneScale, double offsetX, double offsetY, double offsetZ) {
        return new PlaybackModifiers(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds,
                waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ);
    }
}
