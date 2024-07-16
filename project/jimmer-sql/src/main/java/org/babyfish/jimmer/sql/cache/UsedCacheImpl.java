package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

class UsedCacheImpl<K, V> implements UsedCache<K, V> {

    private static final ThreadLocal<Set<UsedCache<?, ?>>> LOADING_CACHES_LOCAL =
        new ThreadLocal<>();

    protected final Cache<K, V> raw;

    private final CacheOperator operator;

    UsedCacheImpl(Cache<K, V> raw, CacheOperator operator) {
        this.raw = Objects.requireNonNull(raw, "raw cannot be null");
        this.operator = operator;
    }

    static <K, V> UsedCache<K, V> wrap(
            Cache<K, V> cache,
            CacheOperator operator
    ) {
        if (cache == null) {
            return null;
        }
        if (cache instanceof UsedCache<?, ?>) {
           UsedCacheImpl<K, V> wrapper = (UsedCacheImpl<K, V>) cache;
           if (wrapper.operator == operator) {
               return wrapper;
           }
           cache = wrapper.raw;
        }
        if (cache instanceof Cache.Parameterized<?, ?>) {
            return new ParameterizedUsedCacheImpl<>(
                    (Cache.Parameterized<K, V>)cache,
                    operator
            );
        }
        return new UsedCacheImpl<>(cache, operator);
    }

    public static <K, V> UsedCache<K, V> export(UsedCache<K, V> cache) {
        if (cache != null) {
            Set<UsedCache<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
            if (disabledCaches != null && disabledCaches.contains(cache)) {
                return null;
            }
        }
        return cache;
    }

    public static <K, V> Cache<K, V> unwrap(Cache<K, V> cache) {
        if (cache instanceof UsedCache<?, ?>) {
            UsedCacheImpl<K, V> wrapper = (UsedCacheImpl<K, V>) cache;
            return wrapper.raw;
        }
        return cache;
    }

    @Override
    public @NotNull ImmutableType type() {
        return raw.type();
    }

    @Override
    public @Nullable ImmutableProp prop() {
        return raw.prop();
    }

    @NotNull
    @Override
    public Map<K, V> getAll(@NotNull Collection<K> keys, @NotNull CacheEnvironment<K, V> env) {
        return loading(() -> {
            Map<K, V> valueMap = raw.getAll(keys, env);
            for (V value : valueMap.values()) {
                validateResult(value);
            }
            return valueMap;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delete(@NotNull K key) {
        if (operator == null || CacheOperator.isSuspending()) {
            raw.delete(key);
        } else {
            operator.delete((UsedCache<Object, ?>) this, key, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delete(@NotNull K key, Object reason) {
        if (operator == null || CacheOperator.isSuspending()) {
            raw.delete(key, reason);
        } else {
            operator.delete((UsedCache<Object, ?>) this, key, reason);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteAll(@NotNull Collection<K> keys) {
        if (keys.isEmpty()) {
            return;
        }
        if (keys.size() == 1) {
            delete(keys.iterator().next());
        } else if (operator == null || CacheOperator.isSuspending()) {
            raw.deleteAll(keys);
        } else {
            operator.deleteAll((UsedCache<Object, ?>) this, (Collection<Object>) keys, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
        if (keys.isEmpty()) {
            return;
        }
        if (keys.size() == 1) {
            delete(keys.iterator().next(), reason);
        } else if (operator == null || CacheOperator.isSuspending()) {
            raw.deleteAll(keys, reason);
        } else {
            operator.deleteAll((UsedCache<Object, ?>) this, (Collection<Object>) keys, reason);
        }
    }

    protected <R> R loading(Supplier<R> block) {
        Set<UsedCache<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
        if (disabledCaches == null) {
            disabledCaches = new HashSet<>();
            LOADING_CACHES_LOCAL.set(disabledCaches);
        }
        disabledCaches.add(this);
        try {
            return block.get();
        } finally {
            disabledCaches.remove(this);
            if (disabledCaches.isEmpty()) {
                LOADING_CACHES_LOCAL.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void validateResult(Object result) {
        ImmutableType type = raw.type();
        ImmutableProp prop = raw.prop();
        if (result == null) {
            if (prop != null && !prop.isReferenceList(TargetLevel.OBJECT) &&!prop.isNullable()) {
                throw new IllegalArgumentException(
                        "Property cache for \"" +
                                prop +
                                "\" must return non-null value"
                );
            }
        } else {
            if (prop == null) {
                if (!(result instanceof ImmutableSpi)) {
                    throw new IllegalArgumentException(
                            "Object cache for \"" +
                                    type +
                                    "\" must return object"
                    );
                }
                if (result instanceof Draft) {
                    throw new IllegalArgumentException(
                            "Object cache for \"" +
                                    type +
                                    "\" cannot return draft"
                    );
                }
            } else if (prop.isReferenceList(TargetLevel.OBJECT)) {
                if (!(result instanceof List<?>)) {
                    throw new IllegalArgumentException(
                            "Association id list cache for \"" +
                                    prop +
                                    "\" must return list"
                    );
                }
                List<Object> rows = (List<Object>) result;
                for (Object row : rows) {
                    if (row instanceof ImmutableSpi || row instanceof List<?>) {
                        throw new IllegalArgumentException(
                                "Association id list cache for \"" +
                                        prop +
                                        "\" returns a list " +
                                        "but some elements are not simple id values"
                        );
                    }
                }
            } else if (result instanceof ImmutableSpi || result instanceof List<?>) {
                throw new IllegalArgumentException(
                        "Property cache for \"" +
                                prop +
                                "\" must return simple value"
                );
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsedCacheImpl<?, ?> that = (UsedCacheImpl<?, ?>) o;
        return raw.equals(that.raw);
    }

    @Override
    public String toString() {
        return "CacheWrapper{" +
                "raw=" + raw +
                '}';
    }
}
