package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

class LockableBinderImpl<K, V> implements LockableBinder<K, V> {

    private final KeyPrefixAwareBinder<K, V> raw;

    private final Locker locker;

    private final Duration waitingDuration;

    private final Duration lockingDuration;

    @SuppressWarnings("unchecked")
    LockableBinderImpl(
            KeyPrefixAwareBinder<K, V> raw,
            Locker locker,
            Duration waitingDuration,
            Duration lockingDuration
    ) {
        if (raw instanceof LockableBinder<?, ?>) {
            this.raw = ((LockableBinder<K, V>)raw).unwrap();
        } else {
            this.raw = Objects.requireNonNull(raw, "raw cannot be null");
        }
        this.locker = Objects.requireNonNull(locker, "locker cannot be null");
        this.waitingDuration = waitingDuration;
        this.lockingDuration = Objects.requireNonNull(lockingDuration, "lockingDuration cannot be null");
    }

    @Override
    public KeyPrefixAwareBinder<K, V> unwrap() {
        return raw;
    }

    @Override
    public Locker locker() {
        return locker;
    }

    @Override
    public @Nullable Duration waitingDuration() {
        return waitingDuration;
    }

    @Override
    public Duration lockingDuration() {
        return lockingDuration;
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
