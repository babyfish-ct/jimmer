package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface LockableBinder<K, V> extends SimpleBinder<K, V> {

    @NotNull
    String keyPrefix();

    @NotNull
    default SimpleBinder<K, V> hardLock(
            @Nullable CacheLocker cacheLocker,
            @Nullable Duration leaseDuration
    ) {
        return lock(cacheLocker, null, leaseDuration);
    }

    @NotNull
    default SimpleBinder<K, V> softLock(
            @Nullable CacheLocker cacheLocker,
            @Nullable Duration leaseDuration
    ) {
        return lock(cacheLocker, Duration.ZERO, leaseDuration);
    }

    @NotNull
    default SimpleBinder<K, V> lock(
            @Nullable CacheLocker cacheLocker,
            @Nullable Duration waitDuration,
            @Nullable Duration leaseDuration
    ) {
        if (cacheLocker == null) {
            return this;
        }
        if (this instanceof LockableBinder.Parameterized<?, ?>) {
            return new ParameterizedLockedSimpleBinder<>(
                    (LockableBinder.Parameterized<K, V>) this,
                    cacheLocker,
                    waitDuration,
                    leaseDuration
            );
        }
        return new LockedSimpleBinder<>(
                this,
                cacheLocker,
                waitDuration,
                leaseDuration
        );
    }

    interface Parameterized<K, V> extends LockableBinder<K, V>, SimpleBinder.Parameterized<K, V> {

        @NotNull
        default SimpleBinder.Parameterized<K, V> hardLock(
                @Nullable CacheLocker cacheLocker,
                @Nullable Duration leaseDuration
        ) {
            return lock(cacheLocker, Duration.ZERO, leaseDuration);
        }

        @NotNull
        default SimpleBinder<K, V> softLock(
                @Nullable CacheLocker cacheLocker,
                @Nullable Duration leaseDuration
        ) {
            return lock(cacheLocker, Duration.ZERO, leaseDuration);
        }

        @NotNull
        default  SimpleBinder.Parameterized<K, V> lock(
                @Nullable CacheLocker cacheLocker,
                @Nullable Duration waitDuration,
                @Nullable Duration leaseDuration
        ) {
            if (cacheLocker == null) {
                return this;
            }
            return new ParameterizedLockedSimpleBinder<>(
                    this,
                    cacheLocker,
                    waitDuration,
                    leaseDuration
            );
        }
    }
}
