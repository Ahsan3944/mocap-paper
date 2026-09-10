package com.ultraop.mocap.playback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player playback synchronization state used by /mocap misc sync. */
public final class PlaybackSynchronization {
    private static final Map<UUID, Boolean> ENABLED = new HashMap<>();

    private PlaybackSynchronization() {}

    public static boolean set(UUID playerId, boolean enabled) {
        boolean old = ENABLED.getOrDefault(playerId, false);
        if (enabled) ENABLED.put(playerId, true);
        else ENABLED.remove(playerId);
        return old;
    }

    public static boolean isEnabled(UUID playerId) {
        return ENABLED.getOrDefault(playerId, false);
    }

    public static void clear() {
        ENABLED.clear();
    }
}
