package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

class LockedSimpleBinder<K, V> implements SimpleBinder<K, V>, LockedBinder<K, V> {

    private final LockableBinder<K, V> raw;

    private final CacheLocker locker;

    private final Duration waitDuration;

    private final Duration leaseDuration;

    @SuppressWarnings("unchecked")
    LockedSimpleBinder(
            LockableBinder<K, V> raw,
            CacheLocker locker,
            Duration waitDuration,
            Duration leaseDuration
    ) {
        if (raw instanceof LockedBinder) {
            raw = (LockableBinder<K, V>)((LockedBinder<K, V>) raw).unwrap();
        }
        if (raw instanceof SimpleBinder.Parameterized<?, ?>) {
            throw new IllegalArgumentException(
                    "The raw binder cannot be \"" + SimpleBinder.Parameterized.class.getName() + "\""
            );
        }
        this.raw = raw;
        this.locker = locker;
        this.waitDuration = waitDuration;
        this.leaseDuration = leaseDuration;
    }

    @Override
    public LockableBinder<K, V> unwrap() {
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
    public @NotNull TrackingMode tracingMode() {
        return raw.tracingMode();
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        return raw.getAll(keys);
    }

    @Override
    public void setAll(Map<K, V> map) {
        raw.setAll(map);
    }

    @Override
    public void deleteAll(Collection<K> keys, Object reason) {
        raw.deleteAll(keys, reason);
    }
}
