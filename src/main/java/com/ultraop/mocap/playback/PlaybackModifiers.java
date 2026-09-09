package com.ultraop.mocap.playback;

/** Per-owner playback modifiers matching the command-facing MoCap model. */
public record PlaybackModifiers(
        String playerName,
        PlayerSkin playerSkin,
        PlayerAsEntity playerAsEntity,
        EntityFilter entityFilter,
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
    public record TransformationConfig(boolean roundBlockPos, RecordingCenter recordingCenter, SceneCenterType sceneCenterType, String sceneCenterSpecific, double centerOffsetX, double centerOffsetY, double centerOffsetZ) {
        public static final TransformationConfig DEFAULT = new TransformationConfig(false, RecordingCenter.AUTO, SceneCenterType.COMMON_FIRST, null, 0, 0, 0);
        public TransformationConfig { recordingCenter = recordingCenter == null ? RecordingCenter.AUTO : recordingCenter; sceneCenterType = sceneCenterType == null ? SceneCenterType.COMMON_FIRST : sceneCenterType; if (sceneCenterType != SceneCenterType.COMMON_SPECIFIC) sceneCenterSpecific = null; }
        public boolean isDefault() { return equals(DEFAULT); }
        public TransformationConfig withRoundBlockPos(boolean v) { return new TransformationConfig(v, recordingCenter, sceneCenterType, sceneCenterSpecific, centerOffsetX, centerOffsetY, centerOffsetZ); }
        public TransformationConfig withRecordingCenter(RecordingCenter v) { return new TransformationConfig(roundBlockPos, v, sceneCenterType, sceneCenterSpecific, centerOffsetX, centerOffsetY, centerOffsetZ); }
        public TransformationConfig withSceneCenter(SceneCenterType v, String s) { return new TransformationConfig(roundBlockPos, recordingCenter, v, s, centerOffsetX, centerOffsetY, centerOffsetZ); }
        public TransformationConfig withCenterOffset(double x, double y, double z) { return new TransformationConfig(roundBlockPos, recordingCenter, sceneCenterType, sceneCenterSpecific, x, y, z); }
    }
    public static final PlaybackModifiers DEFAULT = new PlaybackModifiers(null, PlayerSkin.DEFAULT, PlayerAsEntity.DISABLED, EntityFilter.disabled(), 0, 0, 0, true, false, 0, Mirror.NONE, 1, 1, 0, 0, 0, TransformationConfig.DEFAULT);
    public PlaybackModifiers { playerSkin = playerSkin == null ? PlayerSkin.DEFAULT : playerSkin; playerAsEntity = playerAsEntity == null ? PlayerAsEntity.DISABLED : playerAsEntity; entityFilter = entityFilter == null ? EntityFilter.disabled() : entityFilter; mirror = mirror == null ? Mirror.NONE : mirror; transformationConfig = transformationConfig == null ? TransformationConfig.DEFAULT : transformationConfig; }
    public PlaybackModifiers withPlayerName(String v) { return copy(v, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerSkin(PlayerSkin v) { return copy(playerName, v, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerSkin(String v) { return withPlayerSkin(v == null ? PlayerSkin.DEFAULT : PlayerSkin.fromFile(v)); }
    public PlaybackModifiers withPlayerAsEntity(PlayerAsEntity v) { return copy(playerName, playerSkin, v, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerAsEntity(boolean v) { return withPlayerAsEntity(v ? PlayerAsEntity.enabled(org.bukkit.entity.EntityType.PLAYER, null) : PlayerAsEntity.DISABLED); }
    public PlaybackModifiers withEntityFilter(EntityFilter v) { return copy(playerName, playerSkin, playerAsEntity, v, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withEntityFilter(String v) { return withEntityFilter(new EntityFilter(v)); }
    public PlaybackModifiers withStartDelay(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, v, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitOnStart(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, v, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitOnEnd(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, v, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withWaitForParentEnd(boolean v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, v, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withLoop(boolean v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, v, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withRotation(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, v, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withMirror(Mirror v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, v, playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withPlayerScale(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, v, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withSceneScale(double v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, v, offsetX, offsetY, offsetZ, transformationConfig); }
    public PlaybackModifiers withOffset(double x, double y, double z) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, x, y, z, transformationConfig); }
    public PlaybackModifiers withTransformationConfig(TransformationConfig v) { return copy(playerName, playerSkin, playerAsEntity, entityFilter, startDelaySeconds, waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale, sceneScale, offsetX, offsetY, offsetZ, v); }
    public PlaybackModifiers mergeWithParent(PlaybackModifiers parent) { if (parent == null) return this; return new PlaybackModifiers(playerName != null ? playerName : parent.playerName, playerSkin.mergeWithParent(parent.playerSkin), playerAsEntity.enabled() ? playerAsEntity : parent.playerAsEntity, entityFilter.enabled() ? entityFilter : parent.entityFilter, startDelaySeconds + parent.startDelaySeconds, waitOnStartSeconds + parent.waitOnStartSeconds, waitOnEndSeconds, waitForParentEnd, loop, rotationDegrees, mirror, playerScale * parent.playerScale, sceneScale, offsetX, offsetY, offsetZ, transformationConfig); }
    private static PlaybackModifiers copy(String n, PlayerSkin s, PlayerAsEntity e, EntityFilter f, double sd, double ws, double we, boolean wp, boolean l, double r, Mirror m, double ps, double ss, double x, double y, double z, TransformationConfig c) { return new PlaybackModifiers(n, s, e, f, sd, ws, we, wp, l, r, m, ps, ss, x, y, z, c); }
}
