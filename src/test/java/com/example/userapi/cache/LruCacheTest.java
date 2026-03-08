package com.example.userapi.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LruCacheTest {

    private LruCache<Integer, String> cache;

    @BeforeEach
    void setUp() {
        cache = new LruCache<>(3);
    }

    @Test
    void shouldStoreAndRetrieveValues() {
        cache.put(1, "one");
        cache.put(2, "two");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals(2, cache.size());
    }

    @Test
    void shouldReturnNullForCacheMiss() {
        assertNull(cache.get(99));
    }

    @Test
    void shouldEvictLeastRecentlyUsedWhenCapacityExceeded() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Cache is full (capacity 3). Adding a 4th should evict key 1.
        cache.put(4, "four");

        assertNull(cache.get(1), "Key 1 should have been evicted");
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
        assertEquals(3, cache.size());
    }

    @Test
    void shouldPromoteAccessedEntryAndEvictCorrectOne() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access key 1 to make it recently used
        cache.get(1);

        // Adding key 4 should now evict key 2 (least recently used)
        cache.put(4, "four");

        assertEquals("one", cache.get(1), "Key 1 should still be present after access");
        assertNull(cache.get(2), "Key 2 should have been evicted");
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }

    @Test
    void shouldRemoveEntry() {
        cache.put(1, "one");
        cache.put(2, "two");

        String removed = cache.remove(1);

        assertEquals("one", removed);
        assertNull(cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    void shouldRemoveReturnNullForMissingKey() {
        assertNull(cache.remove(99));
    }

    @Test
    void shouldReportContainsKey() {
        cache.put(1, "one");

        assertTrue(cache.containsKey(1));
        assertFalse(cache.containsKey(2));
    }

    @Test
    void shouldClearAllEntries() {
        cache.put(1, "one");
        cache.put(2, "two");

        cache.clear();

        assertEquals(0, cache.size());
        assertNull(cache.get(1));
        assertNull(cache.get(2));
    }

    @Test
    void shouldReturnCapacity() {
        assertEquals(3, cache.getCapacity());
    }

    @Test
    void shouldThrowOnInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LruCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LruCache<>(-1));
    }

    @Test
    void shouldUpdateExistingKeyValue() {
        cache.put(1, "one");
        cache.put(1, "ONE");

        assertEquals("ONE", cache.get(1));
        assertEquals(1, cache.size());
    }
}
