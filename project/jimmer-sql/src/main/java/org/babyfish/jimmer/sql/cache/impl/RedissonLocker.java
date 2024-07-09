package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.chain.KeyPrefixAwareBinder;
import org.babyfish.jimmer.sql.cache.chain.Locker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.Redisson;
import org.redisson.api.RLock;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedissonLocker implements Locker {

    private final Redisson redisson;

    private final int lockUpgradeThreshold;

    public RedissonLocker(Redisson redisson) {
        this(redisson, 64);
    }

    public RedissonLocker(Redisson redisson, int lockUpgradeThreshold) {
        if (lockUpgradeThreshold < 2) {
            throw new IllegalArgumentException("lockUpgradeThreshold cannot be less than 2");
        }
        this.redisson = Objects.requireNonNull(redisson, "redisson cannot be null");
        this.lockUpgradeThreshold = lockUpgradeThreshold;
    }

    @Override
    public void locking(
            @NotNull KeyPrefixAwareBinder<?, ?> binder,
            @NotNull Set<?> missedKeys,
            @Nullable SortedMap<String, Object> parameterMap,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration,
            Action action
    ) throws InterruptedException {
        String lockPrefix = "$lock$" + binder.keyPrefix();
        if (parameterMap != null) {
            lockPrefix += ":" + parameterMap;
        }
        RLock lock;
        if (missedKeys.size() >= lockUpgradeThreshold) {
            // Too many small locks, merged into one big lock
            lock = redisson.getLock(lockPrefix);
        } else {
            lockPrefix += '-';
            // Sorted lock names can reduce the probability of dead lock
            SortedSet<String> lockNames = new TreeSet<>();
            for (Object missedKey : missedKeys) {
                lockNames.add(lockPrefix + missedKey);
            }
            RLock[] locks = new RLock[lockNames.size()];
            int index = 0;
            for (String lockName : lockNames) {
                locks[index++] = redisson.getLock(lockName);
            }
            lock = redisson.getMultiLock(locks);
        }
        if (waitingDuration == null) { // hard lock
            lock.lockInterruptibly(lockingDuration.toMillis(), TimeUnit.MILLISECONDS);
            try {
                action.execute(true);
            } finally {
                lock.unlock();
            }
        } else { // soft lock
            if (!lock.tryLock(waitingDuration.toMillis(), lockingDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                try {
                    action.execute(true);
                } finally {
                    lock.unlock();
                }
            } else {
                action.execute(false);
            }
        }
    }
}
