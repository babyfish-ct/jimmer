package org.babyfish.jimmer.sql.cache.chain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.SortedMap;

public interface Locker {

    void locking(
            @NotNull KeyPrefixAwareBinder<?, ?> binder,
            @NotNull Set<?> missedKeys,
            @Nullable SortedMap<String, Object> parameterMap,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration,
            Action action
    ) throws Exception;

    @FunctionalInterface
    interface Action {
        void execute(boolean locked);
    }

    static <K, V> SimpleBinder<K, V> hardLockable(
            KeyPrefixAwareBinder<K, V> binder,
            @NotNull Locker locker,
            @NotNull Duration lockingDuration
    ) {
        return lockable(binder, locker, null, lockingDuration);
    }

    static <K, V> SimpleBinder<K, V> hardLockable(
            KeyPrefixAwareBinder.Parameterized<K, V> binder,
            @NotNull Locker locker,
            @NotNull Duration lockingDuration
    ) {
        return lockable(binder, locker, null, lockingDuration);
    }

    static <K, V> SimpleBinder<K, V> softLockable(
            KeyPrefixAwareBinder<K, V> binder,
            @NotNull Locker locker,
            @NotNull Duration lockingDuration
    ) {
        return lockable(binder, locker, Duration.ZERO, lockingDuration);
    }

    static <K, V> SimpleBinder<K, V> softLockable(
            KeyPrefixAwareBinder.Parameterized<K, V> binder,
            @NotNull Locker locker,
            @NotNull Duration lockingDuration
    ) {
        return lockable(binder, locker, Duration.ZERO, lockingDuration);
    }

    static <K, V> SimpleBinder<K, V> lockable(
            KeyPrefixAwareBinder<K, V> binder,
            @NotNull Locker locker,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration
    ) {
        if (binder instanceof KeyPrefixAwareBinder.Parameterized<?, ?>) {
            return new LockableParameterizedBinderImpl<>(
                    (KeyPrefixAwareBinder.Parameterized<K, V>) binder,
                    locker,
                    waitingDuration,
                    lockingDuration
            );
        }
        return new LockableBinderImpl<>(
                binder,
                locker,
                waitingDuration,
                lockingDuration
        );
    }

    static <K, V> @NotNull SimpleBinder<K, V> lockable(
            KeyPrefixAwareBinder.Parameterized<K, V> binder,
            @NotNull Locker locker,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration
    ) {
        return new LockableParameterizedBinderImpl<>(
                binder,
                locker,
                waitingDuration,
                lockingDuration
        );
    }
}
