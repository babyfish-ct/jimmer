package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

public interface CacheCreator {

    @NotNull
    default CacheCreator withRemoteDuration(@Nullable Duration duration) {
        return withRemoteDuration(duration, DEFAULT_REMOTE_DURATION_RANDOM_PERCENT);
    }

    @NotNull
    CacheCreator withRemoteDuration(@Nullable Duration duration, int randomPercent);

    @NotNull
    default CacheCreator withLocalCache(int maxSize) {
        return withLocalCache(maxSize, DEFAULT_LOCAL_DURATION);
    }

    @NotNull
    default CacheCreator withLocalCache(Duration duration) {
        return withLocalCache(DEFAULT_LOCAL_CACHE_MAX_SIZE, duration);
    }

    @NotNull
    CacheCreator withLocalCache(int maxSize, @Nullable Duration duration);

    @NotNull
    default CacheCreator withoutLocalCache() {
        return withLocalCache(0, null);
    }

    @NotNull
    CacheCreator withLock(
            @Nullable CacheLocker locker,
            @Nullable Duration waitDuration,
            @Nullable Duration leaseDuration
    );

    @NotNull
    default CacheCreator withHardLock(
            @Nullable CacheLocker locker,
            @Nullable Duration leaseDuration) {
        return withLock(locker, null, leaseDuration);
    }

    @NotNull
    default CacheCreator withSoftLock(
            @Nullable CacheLocker locker,
            @Nullable Duration leaseDuration
    ) {
        return withLock(locker, Duration.ZERO, leaseDuration);
    }

    @NotNull
    default CacheCreator withoutLock() {
        return withLock(null, null, null);
    }

    @NotNull
    CacheCreator withTracking(
            @Nullable CacheTracker tracker
    );

    @NotNull
    default CacheCreator withoutTracking() {
        return withTracking(null);
    }

    <K, V> Cache<K, V> createForObject(ImmutableType type);

    default <K, V> Cache<K, V> createForProp(ImmutableProp prop) {
        return createForProp(prop, true);
    }

    <K, V> Cache<K, V> createForProp(ImmutableProp prop, boolean multiView);

    Duration DEFAULT_REMOTE_DURATION = Duration.ofMinutes(30);

    int DEFAULT_REMOTE_DURATION_RANDOM_PERCENT = 30;

    int DEFAULT_LOCAL_CACHE_MAX_SIZE = 100;

    Duration DEFAULT_LOCAL_DURATION = Duration.ofMinutes(1);
}

