package com.ultraop.mocap.playback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side custom-skin byte cache limits matching MoCap's upstream safety limits. */
public final class CustomSkinCache {
    public static final int MAX_ACCEPTED_FILE_SIZE = 20 * (1 << 20);
    public static final long MAX_CUMULATIVE_ARRAY_SIZE = 128L * (1 << 20);
    public static final int MAX_ELEMENT_COUNT = 4096;
    public static final int MAX_TEXTURE_WIDTH = 4096;
    public static final int MAX_TEXTURE_HEIGHT = 4096;

    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    private long sizeSum;

    public synchronized byte[] get(String key) {
        return cache.get(key);
    }

    public synchronized boolean put(String key, byte[] value) {
        if (value == null || value.length > MAX_ACCEPTED_FILE_SIZE) return false;
        if (sizeSum + value.length > MAX_CUMULATIVE_ARRAY_SIZE || cache.size() + 1 > MAX_ELEMENT_COUNT) {
            clear();
        }
        cache.put(key, value);
        sizeSum += value.length;
        return true;
    }

    public synchronized void clear() {
        cache.clear();
        sizeSum = 0L;
    }

    public static boolean validTextureDimensions(int width, int height) {
        return width > 0 && height > 0 && width <= MAX_TEXTURE_WIDTH && height <= MAX_TEXTURE_HEIGHT;
    }

    public synchronized long sizeBytes() {
        return sizeSum;
    }

    public synchronized int size() {
        return cache.size();
    }
}
