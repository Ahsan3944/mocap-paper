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
    private final List<PlayerStateFrame> frames = new ArrayList<>();

    private long nextTick;
    private Instant stoppedAt;

    public RecordingSession(Player player) {
        this.id = UUID.randomUUID();
        this.sourcePlayerId = player.getUniqueId();
        this.sourcePlayerName = player.getName();
        this.startedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourcePlayerId() {
        return sourcePlayerId;
    }

    public String getSourcePlayerName() {
        return sourcePlayerName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getStoppedAt() {
        return stoppedAt;
    }

    public long getDurationTicks() {
        return frames.size();
    }

    public List<PlayerStateFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    void capture(Player player) {
        frames.add(PlayerStateFrame.capture(player, nextTick++));
    }

    void stop() {
        if (stoppedAt == null) {
            stoppedAt = Instant.now();
        }
    }
}
