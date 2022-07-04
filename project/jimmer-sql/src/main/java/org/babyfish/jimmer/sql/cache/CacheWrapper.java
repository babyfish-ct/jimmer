package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.*;
import java.util.function.Supplier;

class CacheWrapper<K, V> implements Cache<K, V> {

    private static final ThreadLocal<Set<Cache<?, ?>>> LOADING_CACHES_LOCAL =
        new ThreadLocal<>();

    private final Cache<K, V> raw;

    private final Type type;

    private CacheWrapper(Cache<K, V> raw, Type type) {
        this.raw = raw;
        this.type = type;
    }

    public static <K, V> Cache<K, V> wrap(Cache<K, V> cache, Type type) {
        if (cache == null) {
            return null;
        }
        if (cache instanceof CacheWrapper<?, ?>) {
            if (((CacheWrapper<?, ?>) cache).type != type) {
                throw new AssertionError("");
            }
        }
        Set<Cache<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
        if (disabledCaches != null && disabledCaches.contains(cache)) {
            return null;
        }
        return new CacheWrapper<>(cache, type);
    }

    public static <K, V> Cache<K, V> unwrap(Cache<K, V> cache) {
        if (cache instanceof CacheWrapper<?, ?>) {
            CacheWrapper<K, V> wrapper = (CacheWrapper<K, V>) cache;
            return wrapper.raw;
        }
        return cache;
    }


    @Override
    public V get(K key, QueryCacheEnvironment<K, V> env) {
        return execute(() -> validateResult(raw.get(key, env)));
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys, QueryCacheEnvironment<K, V> env) {
        return execute(() -> validateResult(raw.getAll(keys, env)));
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
        Set<Cache<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
        if (disabledCaches == null) {
            disabledCaches = Collections.singleton(raw);
            LOADING_CACHES_LOCAL.set(disabledCaches);
        } else if (disabledCaches.size() == 1) {
            Cache<?, ?> oldRaw = disabledCaches.iterator().next();
            disabledCaches = new HashSet<>();
            disabledCaches.add(oldRaw);
            disabledCaches.add(raw);
            LOADING_CACHES_LOCAL.set(disabledCaches);
        } else {
            disabledCaches.add(raw);
        }
        try {
            return block.get();
        } finally {
            if (disabledCaches.size() < 2) {
                LOADING_CACHES_LOCAL.remove();
            } else {
                disabledCaches.remove(raw);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R validateResult(R result) {
        switch (type) {
            case ASSOCIATED_ID:
                for (Object value : ((Map<?, Object>)result).values()) {
                    validateValue(value);
                }
                break;
            case ASSOCIATED_ID_LIST:
                for (List<Object> list : ((Map<?, List<Object>>)result).values()) {
                    if (list != null) {
                        for (Object value : list) {
                            validateValue(value);
                        }
                    }
                }
                break;
        }
        return result;
    }

    private void validateValue(Object value) {
        if (value instanceof ImmutableSpi) {
            throw new IllegalArgumentException(
                    "Illegal cache " +
                            raw +
                            ", " +
                            type.getName() +
                            " can only return id, should not return object"
            );
        }
    }

    enum Type {

        OBJECT("object cache"),
        ASSOCIATED_ID("associated id ache"),
        ASSOCIATED_ID_LIST("associated id list cache");

        private String name;

        private Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
