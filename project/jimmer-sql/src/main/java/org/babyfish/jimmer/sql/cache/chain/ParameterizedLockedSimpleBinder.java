package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

class ParameterizedLockedSimpleBinder<K, V> implements SimpleBinder.Parameterized<K, V>, LockedBinder<K, V> {

    private final LockableBinder.Parameterized<K, V> raw;

    private final CacheLocker locker;

    private final Duration waitDuration;

    private final Duration leaseDuration;

    @SuppressWarnings("unchecked")
    ParameterizedLockedSimpleBinder(
            LockableBinder.Parameterized<K, V> raw,
            CacheLocker locker,
            Duration waitDuration,
            Duration leaseDuration
    ) {
        if (raw instanceof LockedBinder) {
            raw = (LockableBinder.Parameterized<K, V>)((LockedBinder) raw).unwrap();
        }
        this.raw = raw;
        this.locker = locker;
        this.waitDuration = waitDuration;
        this.leaseDuration = leaseDuration;
    }

    @Override
    public LockableBinder.Parameterized<K, V> unwrap() {
        return raw;
    }

    @Override
    public CacheLocker locker() {
        return locker;
    }

    @Override
    public Duration waitDuration() {
        return waitDuration;
    }

    @Override
    public Duration leaseDuration() {
        return leaseDuration;
    }

    @Override
    public @Nullable ImmutableType type() {
        return raw.type();
    }

    @Override
    public @Nullable ImmutableProp prop() {
        return raw.prop();
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys, SortedMap<String, Object> parameterMap) {
        return raw.getAll(keys, parameterMap);
    }

    @Override
    public void setAll(Map<K, V> map, SortedMap<String, Object> parameterMap) {
        raw.setAll(map, parameterMap);
    }

    @Override
    public void deleteAll(Collection<K> keys, Object reason) {
        raw.deleteAll(keys, reason);
    }
}
