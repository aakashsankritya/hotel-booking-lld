package com.rupeek.hotel.application.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * One lock per key. Callers with the same key run one after the other. Callers with different keys run in parallel.
 * Locks are never removed (see "With more time" in the README).
 */
public final class KeyedLocks<K> {

    private final Map<K, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(K key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void runWithLock(K key, Runnable action) {
        withLock(key, () -> {
            action.run();
            return null;
        });
    }
}
