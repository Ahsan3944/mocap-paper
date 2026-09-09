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
        double offsetZ,
        TransformationConfig transformationConfig) {

    public enum Mirror { NONE, X, Z, XZ }

    public enum RecordingCenter { AUTO, BLOCK_CENTER, BLOCK_CORNER, ACTUAL }

    public enum SceneCenterType { COMMON_FIRST, COMMON_LAST, COMMON_SPECIFIC, INDIVIDUAL }

    public record TransformationConfig(
            boolean roundBlockPos,
            RecordingCenter recordingCenter,
            SceneCenterType sceneCenterType,
            String sceneCenterSpecific,
            double centerOffsetX,
            double centerOffsetY,
            double centerOffsetZ) {
        public static final TransformationConfig DEFAULT = new TransformationConfig(
                false, RecordingCenter.AUTO, SceneCenterType.COMMON_FIRST, null, 0.0, 0.0, 0.0);

        public TransformationConfig {
            recordingCenter = recordingCenter == null ? RecordingCenter.AUTO : recordingCenter;
            sceneCenterType = sceneCenterType == null ? SceneCenterType.COMMON_FIRST : sceneCenterType;
            if (sceneCenterType != SceneCenterType.COMMON_SPECIFIC) sceneCenterSpecific = null;
        }

        public boolean isDefault() {
            return equals(DEFAULT);
        }

        public TransformationConfig withRoundBlockPos(boolean value) {
            return new TransformationConfig(value, recordingCenter, sceneCenterType, sceneCenterSpecific,
                    centerOffsetX, centerOffsetY, centerOffsetZ);
        }

        public TransformationConfig withRecordingCenter(RecordingCenter value) {
            return new TransformationConfig(roundBlockPos, value, sceneCenterType, sceneCenterSpecific,
                    centerOffsetX, centerOffsetY, centerOffsetZ);
        }

        public TransformationConfig withSceneCenter(SceneCenterType value, String specific) {
            return new TransformationConfig(roundBlockPos, recordingCenter, value, specific,
                    centerOffsetX, centerOffsetY, centerOffsetZ);
        }

        public TransformationConfig withCenterOffset(double x, double y, double z) {
            return new TransformationConfig(roundBlockPos, recordingCenter, sceneCenterType, sceneCenterSpecific,
                    x, y, z);
        }
    }

    public static final PlaybackModifiers DEFAULT = new PlaybackModifiers(
            null, null, false, null,
            0.0, 0.0, 0.0, true, false, 0.0, Mirror.NONE,
            1.0, 1.0, 0.0, 0.0, 0.0, TransformationConfig.DEFAULT);

    public PlaybackModifiers withPlayerName(String value) { return copy(value, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerSkin(String value) { return copy(playerName, value, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerAsEntity(boolean value) { return copy(playerName, playerSkin, value, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withEntityFilter(String value) { return copy(playerName, playerSkin, playerAsEntity, value, startDelaySeconds, waitOnStartSeconds, waitForParentEnd ? entityFilter : entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withStartDelay(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, value, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitOnStart(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, value, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitOnEnd(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, value, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitForParentEnd(boolean value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, value, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withLoop(boolean value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, value, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withRotation(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, value, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withMirror(Mirror value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, value, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerScale(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, value, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withSceneScale(double value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, value, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withOffset(double x, double y, double z) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, x, y, z, transformationConfig); }
    public PlaybackModifiers withTransformationConfig(TransformationConfig value) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, value); }

    public PlaybackModifiers mergeWithParent(PlaybackModifiers parent) {
        if (parent == null) return this;
        return new PlaybackModifiers(
                playerName != null ? playerName : parent.playerName,
                playerSkin != null ? playerSkin : parent.playerSkin,
                playerAsEntity || parent.playerAsEntity,
                entityFilter != null ? entityFilter : parent.entityFilter,
                startDelaySeconds + parent.startDelaySeconds,
                waitOnStartSeconds + parent.waitOnStartSeconds,
                waitOnEndSeconds,
                waitForParentEnd,
                loop,
                rotationDegrees,
                mirror,
                playerScale * parent.playerScale,
                sceneScale,
                offsetX,
                offsetY,
                offsetZ,
                transformationConfig);
    }

    private static PlaybackModifiers copy(String playerName, String playerSkin, boolean playerAsEntity, String entityFilter,
                                          double startDelaySeconds, double waitOnStartSeconds, double waitOnEndSeconds,
                                          boolean waitForParentEnd, boolean loop, double rotationDegrees, Mirror mirror,
                                          double playerScale, double sceneScale, double offsetX, double offsetY, double offsetZ,
                                          TransformationConfig transformationConfig) {
        return new PlaybackModifiers(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds,
                waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ,
                transformationConfig == null ? TransformationConfig.DEFAULT : transformationConfig);
    }
}
