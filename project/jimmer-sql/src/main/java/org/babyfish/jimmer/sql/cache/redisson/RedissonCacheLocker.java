package org.babyfish.jimmer.sql.cache.redisson;

import org.babyfish.jimmer.sql.cache.chain.LockableBinder;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedissonCacheLocker implements CacheLocker {

    private final RedissonClient redissonClient;

    private final int lockUpgradeThreshold;

    public RedissonCacheLocker(RedissonClient redissonClient) {
        this(redissonClient, 64);
    }

    public RedissonCacheLocker(RedissonClient redissonClient, int lockUpgradeThreshold) {
        if (lockUpgradeThreshold < 2) {
            throw new IllegalArgumentException("lockUpgradeThreshold cannot be less than 2");
        }
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient cannot be null");
        this.lockUpgradeThreshold = lockUpgradeThreshold;
    }

    @Override
    public void locking(
            @NotNull LockableBinder<?, ?> binder,
            @NotNull Set<?> missedKeys,
            @Nullable Duration waitingDuration,
            @NotNull Duration lockingDuration,
            Action action
    ) throws InterruptedException {
        String lockPrefix = "$lock$" + binder.keyPrefix();
        RLock lock;
        if (missedKeys.size() >= lockUpgradeThreshold) {
            // Too many small locks, merged into one big lock
            lock = redissonClient.getLock(lockPrefix);
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
                locks[index++] = redissonClient.getLock(lockName);
            }
            lock = redissonClient.getMultiLock(locks);
        }
        if (waitingDuration == null) { // hard lock
            lock.lockInterruptibly(lockingDuration.toMillis(), TimeUnit.MILLISECONDS);
            try {
                action.execute(true);
            } finally {
                lock.unlock();
            }
        } else { // soft lock
            if (lock.tryLock(waitingDuration.toMillis(), lockingDuration.toMillis(), TimeUnit.MILLISECONDS)) {
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
