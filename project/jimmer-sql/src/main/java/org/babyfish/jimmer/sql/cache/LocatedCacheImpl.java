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

class LocatedCacheImpl<K, V> implements LocatedCache<K, V> {

    private static final ThreadLocal<Set<LocatedCacheImpl<?, ?>>> LOADING_CACHES_LOCAL =
        new ThreadLocal<>();

    private final Cache<K, V> raw;

    private final ImmutableType type;

    private final ImmutableProp prop;

    public LocatedCacheImpl(Cache<K, V> raw, ImmutableType type, ImmutableProp prop) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("The nullity of type and prop must be different");
        }
        if (prop != null && !prop.isAssociation(TargetLevel.ENTITY) && !prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "The prop \"" + prop + "\" is neither entity association nor transient property with resolver"
            );
        }
        this.raw = Objects.requireNonNull(raw, "raw cannot be null");
        this.type = type;
        this.prop = prop;
    }

    public static <K, V> LocatedCacheImpl<K, V> wrap(
            Cache<K, V> cache,
            ImmutableType type
    ) {
        return wrap(cache, type, null);
    }

    public static <K, V> LocatedCacheImpl<K, V> wrap(
            Cache<K, V> cache,
            ImmutableProp prop
    ) {
        return wrap(cache, null, prop);
    }

    private static <K, V> LocatedCacheImpl<K, V> wrap(
            Cache<K, V> cache,
            ImmutableType type,
            ImmutableProp prop
    ) {
        if (cache == null) {
            return null;
        }
        if (cache instanceof LocatedCache<?, ?>) {
            LocatedCacheImpl<K, V> wrapper = (LocatedCacheImpl<K, V>) cache;
            if (wrapper.type == type && wrapper.prop == prop) {
                return wrapper;
            }
            cache = ((LocatedCacheImpl<K, V>) cache).raw;
        }
        return new LocatedCacheImpl<>(cache, type, prop);
    }

    public static <K, V> LocatedCache<K, V> export(LocatedCache<K, V> cacheWrapper) {
        if (cacheWrapper != null) {
            Set<LocatedCacheImpl<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
            if (disabledCaches != null && disabledCaches.contains(cacheWrapper)) {
                return null;
            }
        }
        return cacheWrapper;
    }

    public static <K, V> Cache<K, V> unwrap(Cache<K, V> cache) {
        if (cache instanceof LocatedCacheImpl<?, ?>) {
            LocatedCacheImpl<K, V> wrapper = (LocatedCacheImpl<K, V>) cache;
            return wrapper.raw;
        }
        return cache;
    }

    @Override
    public ImmutableType getType() {
        return type;
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
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

    @Override
    public void deleteAll(@NotNull Collection<K> keys, @Nullable Object reason) {
        raw.deleteAll(keys, reason);
    }

    private <R> R loading(Supplier<R> block) {
        Set<LocatedCacheImpl<?, ?>> disabledCaches = LOADING_CACHES_LOCAL.get();
        if (disabledCaches == null) {
            disabledCaches = new HashSet<>();
            LOADING_CACHES_LOCAL.set(disabledCaches);
        }
        disabledCaches.add(this);
        try {
            return block.get();
        } finally {
            if (disabledCaches.size() > 1) {
                disabledCaches.remove(this);
            } else {
                LOADING_CACHES_LOCAL.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateResult(Object result) {
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
        return Objects.hash(raw, type, prop);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocatedCacheImpl<?, ?> that = (LocatedCacheImpl<?, ?>) o;
        return raw.equals(that.raw) && Objects.equals(type, that.type) && Objects.equals(prop, that.prop);
    }

    @Override
    public String toString() {
        return "CacheWrapper{" +
                "raw=" + raw +
                ", type=" + type +
                ", prop=" + prop +
                '}';
    }
}
