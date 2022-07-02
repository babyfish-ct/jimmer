package org.babyfish.jimmer.sql.cache;

import java.util.*;
import java.util.function.Supplier;

class ReentrantCache<K, V> implements Cache<K, V> {

    private static final ThreadLocal<Set<Cache<?, ?>>> DISABLED_CACHES_LOCAL =
        new ThreadLocal<>();

    private final Cache<K, V> raw;

    private ReentrantCache(Cache<K, V> raw) {
        this.raw = raw;
    }

    public static <K, V> Cache<K, V> reentrant(Cache<K, V> cache) {
        if (cache == null || cache instanceof ReentrantCache<?, ?>) {
            return null;
        }
        Set<Cache<?, ?>> disabledCaches = DISABLED_CACHES_LOCAL.get();
        if (disabledCaches != null && disabledCaches.contains(cache)) {
            return null;
        }
        return new ReentrantCache<>(cache);
    }

    @Override
    public V get(K key, CacheEnvironment env) {
        return execute(() -> raw.get(key, env));
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys, CacheEnvironment env) {
        return execute(() -> raw.getAll(keys, env));
    }

    @Override
    public void delete(K key, CacheEnvironment env) {
        this.<Void>execute(() -> {
            raw.delete(key, env);
            return null;
        });
    }

    @Override
    public void deleteAll(Collection<K> keys, CacheEnvironment env) {
        this.<Void>execute(() -> {
            raw.deleteAll(keys, env);
            return null;
        });
    }

    private <R> R execute(Supplier<R> block) {
        Set<Cache<?, ?>> disabledCaches = DISABLED_CACHES_LOCAL.get();
        if (disabledCaches == null) {
            disabledCaches = Collections.singleton(raw);
            DISABLED_CACHES_LOCAL.set(disabledCaches);
        } else if (disabledCaches.size() == 1) {
            Cache<?, ?> oldRaw = disabledCaches.iterator().next();
            disabledCaches = new HashSet<>();
            disabledCaches.add(oldRaw);
            disabledCaches.add(raw);
            DISABLED_CACHES_LOCAL.set(disabledCaches);
        } else {
            disabledCaches.add(raw);
        }
        try {
            return block.get();
        } finally {
            if (disabledCaches.size() < 2) {
                DISABLED_CACHES_LOCAL.remove();
            } else {
                disabledCaches.remove(raw);
            }
        }
    }
}
