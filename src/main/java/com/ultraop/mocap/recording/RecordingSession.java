package com.ultraop.mocap.recording;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** In-memory recording produced by one recording session. */
public final class RecordingSession {
    private final UUID id;
    private final UUID sourcePlayerId;
    private final String sourcePlayerName;
    private final Instant startedAt;
    private final List<PlayerStateFrame> frames;
    private String instantSaveName;
    private long nextTick;
    private Instant stoppedAt;

    public RecordingSession(Player player) {
        this(UUID.randomUUID(), player.getUniqueId(), player.getName(), Instant.now(), null, new ArrayList<>());
    }

    private RecordingSession(UUID id, UUID sourcePlayerId, String sourcePlayerName,
                             Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames) {
        this.id = id;
        this.sourcePlayerId = sourcePlayerId;
        this.sourcePlayerName = sourcePlayerName;
        this.startedAt = startedAt;
        this.stoppedAt = stoppedAt;
        this.frames = new ArrayList<>(frames);
        this.nextTick = frames.stream().mapToLong(PlayerStateFrame::tick).max().orElse(-1L) + 1L;
    }

    static RecordingSession loaded(UUID id, UUID sourcePlayerId, String sourcePlayerName,
                                   Instant startedAt, Instant stoppedAt, List<PlayerStateFrame> frames) {
        return new RecordingSession(id, sourcePlayerId, sourcePlayerName, startedAt, stoppedAt, frames);
    }

    public UUID getId() { return id; }
    public UUID getSourcePlayerId() { return sourcePlayerId; }
    public String getSourcePlayerName() { return sourcePlayerName; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public long getDurationTicks() { return frames.size(); }
    public List<PlayerStateFrame> getFrames() { return Collections.unmodifiableList(frames); }
    public String getInstantSaveName() { return instantSaveName; }
    public void setInstantSaveName(String instantSaveName) { this.instantSaveName = instantSaveName; }

    void capture(Player player) {
        frames.add(PlayerStateFrame.capture(player, nextTick++));
    }

    void stop() {
        if (stoppedAt == null) {
            stoppedAt = Instant.now();
        }
    }
}
