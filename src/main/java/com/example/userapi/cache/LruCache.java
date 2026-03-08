package com.example.userapi.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A generic thread-safe LRU (Least Recently Used) cache.
 * <p>
 * Uses a {@link LinkedHashMap} with access-order to automatically
 * evict the least recently used entry when the capacity is exceeded.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class LruCache<K, V> {

    private final Map<K, V> cache;
    private final int capacity;

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cache capacity must be positive, got: " + capacity);
        }
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return cache.get(key);
    }

    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }

    public synchronized V remove(K key) {
        return cache.remove(key);
    }

    public synchronized boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public synchronized int size() {
        return cache.size();
    }

    public synchronized void clear() {
        cache.clear();
    }

    public int getCapacity() {
        return capacity;
    }
}
