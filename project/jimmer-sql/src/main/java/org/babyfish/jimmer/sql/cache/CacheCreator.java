package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface CacheCreator {

    /**
     * Set L2 cache expiration time.
     *
     * <p>This method does not modify the current object, but creates a new object.</p>
     *
     * <p>Note: This method is equivalent to {@code withRemoteDuration(duration, 30)}</p>
     *
     * @param duration The expiration time
     * @return The created new object
     */
    @NewChain
    @NotNull
    default CacheCreator withRemoteDuration(@Nullable Duration duration) {
        return withRemoteDuration(duration, DEFAULT_REMOTE_DURATION_RANDOM_PERCENT);
    }

    /**
     * Set L2 cache expiration time.
     *
     * <p>This method does not modify the current object, but creates a new object.</p>
     *
     * @param duration The expiration time
     * @param randomPercent A random percent
     *     <p>Batch loading is a frequently used optimization method in jimmer,
     *     so a batch of values are often put into the cache at the same time.
     *     If the expiration time of these cached data is the same,
     *     then in the future, some affected data will be discarded by the cache
     *     at the same time so that a big hole will appear in the defense of the cache,
     *     which is easy to be penetrated.</p>
     *
     *     <p>To solve this problem, Jimmer supports a random factor that modifies
     *     the expiration time and then applies it to the L2 cache,
     *     ultimately making the expiration time of the data uneven.</p>
     * @return The created new object
     */
    @NewChain
    @NotNull
    CacheCreator withRemoteDuration(@Nullable Duration duration, int randomPercent);

    @NewChain
    @NotNull
    default CacheCreator withLocalCache(int maxSize) {
        return withLocalCache(maxSize, DEFAULT_LOCAL_DURATION);
    }

    @NewChain
    @NotNull
    default CacheCreator withLocalCache(Duration duration) {
        return withLocalCache(DEFAULT_LOCAL_CACHE_MAX_SIZE, duration);
    }

    @NewChain
    @NotNull
    CacheCreator withLocalCache(int maxSize, @Nullable Duration duration);

    @NewChain
    @NotNull
    default CacheCreator withoutLocalCache() {
        return withLocalCache(0, null);
    }

    @NewChain
    @NotNull
    CacheCreator withLock(
            @Nullable CacheLocker locker,
            @Nullable Duration waitDuration,
            @Nullable Duration leaseDuration
    );

    @NewChain
    @NotNull
    default CacheCreator withHardLock(
            @Nullable CacheLocker locker,
            @Nullable Duration leaseDuration) {
        return withLock(locker, null, leaseDuration);
    }

    @NewChain
    @NotNull
    default CacheCreator withSoftLock(
            @Nullable CacheLocker locker,
            @Nullable Duration leaseDuration
    ) {
        return withLock(locker, Duration.ZERO, leaseDuration);
    }

    @NewChain
    @NotNull
    default CacheCreator withoutLock() {
        return withLock(null, null, null);
    }

    @NewChain
    @NotNull
    CacheCreator withTracking(
            @Nullable CacheTracker tracker
    );

    @NewChain
    @NotNull
    default CacheCreator withoutTracking() {
        return withTracking(null);
    }

    @NewChain
    @NotNull
    default CacheCreator withMultiViewProperties(
            @Nullable Integer localMaximumSize,
            @Nullable Duration localDuration
    ) {
        return withMultiViewProperties(localMaximumSize, localDuration, null);
    }

    @NewChain
    @NotNull
    CacheCreator withMultiViewProperties(
            @Nullable Integer localMaximumSize,
            @Nullable Duration localDuration,
            @Nullable Duration remoteDuration
    );

    <K, V> Cache<K, V> createForObject(ImmutableType type);

    <K, V> Cache<K, V> createForProp(ImmutableProp prop, boolean multiView);

    Duration DEFAULT_REMOTE_DURATION = Duration.ofMinutes(30);

    int DEFAULT_REMOTE_DURATION_RANDOM_PERCENT = 30;

    int DEFAULT_LOCAL_CACHE_MAX_SIZE = 100;

    Duration DEFAULT_LOCAL_DURATION = Duration.ofMinutes(1);
}

