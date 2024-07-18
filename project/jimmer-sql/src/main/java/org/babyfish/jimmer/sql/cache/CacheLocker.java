package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.cache.chain.LockableBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Set;
import java.util.SortedMap;

public interface CacheLocker {

    void locking(
            @NotNull LockableBinder<?, ?> binder,
            @NotNull Set<?> missedKeys,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration,
            Action action
    ) throws Exception;

    @FunctionalInterface
    interface Action {
        void execute(boolean locked);
    }
}
