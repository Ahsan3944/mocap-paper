package com.ultraop.mocap.recording;

import com.ultraop.mocap.playback.EntityFilter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** In-memory recording produced by one recording session. */
public final class RecordingSession {
    private static final double ENTITY_TRACKING_DISTANCE = 128.0;
    private static final EntityFilter TRACK_ENTITIES = EntityFilter.DEFAULT_TRACK_ENTITIES;

    private final UUID id;
    private final UUID sourcePlayerId;
    private final String sourcePlayerName;
    private final Instant startedAt;
    private final List<PlayerStateFrame> frames;
    private final Map<UUID, List<EntityStateFrame>> entityFrames;
    private final Map<UUID, Integer> trackedLastSeenTick;
    private String instantSaveName;
    private long nextTick;
    private Instant stoppedAt;

    public RecordingSession(Player player) {
        this(UUID.randomUUID(), player.getUniqueId(), player.getName(), Instant.now(), null,
                new ArrayList<>(), new LinkedHashMap<>());
    }

    private RecordingSession(UUID id, UUID sourcePlayerId, String sourcePlayerName,
                             Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames,
                             Map<UUID, List<EntityStateFrame>> entityFrames) {
        this.id = id;
        this.sourcePlayerId = sourcePlayerId;
        this.sourcePlayerName = sourcePlayerName;
        this.startedAt = startedAt;
        this.stoppedAt = stoppedAt;
        this.frames = new ArrayList<>(frames);
        this.entityFrames = new LinkedHashMap<>();
        entityFrames.forEach((uuid, values) -> this.entityFrames.put(uuid, new ArrayList<>(values)));
        this.trackedLastSeenTick = new LinkedHashMap<>();
        this.entityFrames.forEach((uuid, values) -> {
            if (!values.isEmpty()) this.trackedLastSeenTick.put(uuid, Math.toIntExact(values.get(values.size() - 1).tick()));
        });
        this.nextTick = frames.stream().mapToLong(PlayerStateFrame::tick).max().orElse(-1L) + 1L;
    }

    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName,
                                   Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames) {
        return new RecordingSession(id, sourcePlayerId, sourcePlayerName, startedAt, stoppedAt, frames, Map.of());
    }

    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName,
                                   Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames,
                                   Map<UUID, List<EntityStateFrame>> entityFrames) {
        return new RecordingSession(id, sourcePlayerId, sourcePlayerName, startedAt, stoppedAt, frames, entityFrames);
    }

    public UUID getId() { return id; }
    public UUID getSourcePlayerId() { return sourcePlayerId; }
    public String getSourcePlayerName() { return sourcePlayerName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public long getDurationTicks() { return frames.size(); }
    public List<PlayerStateFrame> getFrames() { return Collections.unmodifiableList(frames); }
    public Map<UUID, List<EntityStateFrame>> getEntityFrames() {
        Map<UUID, List<EntityStateFrame>> copy = new LinkedHashMap<>();
        entityFrames.forEach((uuid, values) -> copy.put(uuid, Collections.unmodifiableList(values)));
        return Collections.unmodifiableMap(copy);
    }
    public String getInstantSaveName() { return instantSaveName; }
    public void setInstantSaveName(String instantSaveName) { this.instantSaveName = instantSaveName; }

    void capture(Player player) {
        frames.add(PlayerStateFrame.capture(player, nextTick));
        trackEntities(player);
        nextTick++;
    }

    private void trackEntities(Player player) {
        double maxDistanceSquared = ENTITY_TRACKING_DISTANCE * ENTITY_TRACKING_DISTANCE;
        Set<UUID> seenThisTick = new HashSet<>();
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof Player || !entity.isValid()) continue;
            if (entity.getUniqueId().equals(sourcePlayerId)) continue;
            if (player.getLocation().distanceSquared(entity.getLocation()) > maxDistanceSquared) continue;
            if (isPlaybackEntity(entity)) continue;
            if (!TRACK_ENTITIES.matches(entity)) continue;

            UUID entityId = entity.getUniqueId();
            seenThisTick.add(entityId);
            entityFrames.computeIfAbsent(entityId, ignored -> new ArrayList<>())
                    .add(EntityStateFrame.capture(entity, nextTick));
            trackedLastSeenTick.put(entityId, Math.toIntExact(nextTick));
        }

        // Keep historical frames for entities that disappeared from tracking range. Their last
        // frame is the point at which playback can later remove the actor.
        trackedLastSeenTick.keySet().removeIf(uuid -> !seenThisTick.contains(uuid));
    }

    private static boolean isPlaybackEntity(Entity entity) {
        return entity.getScoreboardTags().stream().anyMatch(tag ->
                tag.equals("mocap_entity") || tag.equals("mocap:entity"));
    }

    void stop() {
        trackedLastSeenTick.clear();
        if (stoppedAt == null) stoppedAt = Instant.now();
    }
}
