package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.cache.CacheCreator;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.RemoteKeyPrefixProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Objects;

public abstract class AbstractCacheCreator implements CacheCreator {

    protected final Cfg cfg;

    private Args args;

    protected AbstractCacheCreator(Cfg cfg) {
        this.cfg = cfg;
    }

    @NewChain
    @NotNull
    @Override
    public CacheCreator withRemoteDuration(@Nullable Duration duration, int randomPercent) {
        return newCacheCreator(
                new RemoteDuration(cfg, duration, randomPercent)
        );
    }

    @NewChain
    @NotNull
    @Override
    public CacheCreator withLocalCache(int maximumSize, Duration duration) {
        return newCacheCreator(
                new LocalCache(cfg, maximumSize, duration)
        );
    }

    @NewChain
    @NotNull
    @Override
    public CacheCreator withLock(
            @Nullable CacheLocker locker,
            @Nullable Duration waitDuration,
            @Nullable Duration leaseDuration
    ) {
        return newCacheCreator(
                new Lock(cfg, locker, waitDuration, leaseDuration)
        );
    }

    @NewChain
    @NotNull
    @Override
    public CacheCreator withTracking(@Nullable CacheTracker tracker) {
        return newCacheCreator(
                new Tracking(cfg, tracker)
        );
    }

    @NewChain
    @NotNull
    @Override
    public CacheCreator withMultiViewProperties(
            @Nullable Integer localMaximumSize,
            @Nullable Duration localDuration,
            @Nullable Duration remoteDuration
    ) {
        return newCacheCreator(
                new MultiViewProperties(
                        cfg,
                        localMaximumSize,
                        localDuration,
                        remoteDuration
                )
        );
    }

    @SuppressWarnings("unchecked")
    protected final <A extends Args> A args() {
        Args args = this.args;
        if (args == null) {
            this.args = args = newArgs(cfg);
        }
        return (A) args;
    }

    protected abstract Args newArgs(Cfg cfg);

    protected abstract CacheCreator newCacheCreator(Cfg cfg);

    protected static abstract class Cfg {

        final Cfg prev;

        protected Cfg(Cfg prev) {
            this.prev = prev;
        }

        @SuppressWarnings("unchecked")
        public <T extends Cfg> T as(Class<T> type) {
            if (this.getClass() == type) {
                return (T) this;
            }
            if (prev != null) {
                return prev.as(type);
            }
            return null;
        }
    }

    private static class Root extends Cfg {

        final RedisConnectionFactory connectionFactory;

        final JsonCodec<?> jsonCodec;

        private Root(RedisConnectionFactory connectionFactory, JsonCodec<?> jsonCodec) {
            super(null);
            this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory cannot be null");
            this.jsonCodec = jsonCodec;
        }
    }

    private static class RemoteDuration extends Cfg {

        final Duration duration;

        int randomPercent;

        RemoteDuration(Cfg prev, Duration duration, int randomPercent) {
            super(prev);
            if (duration == null) {
                duration = DEFAULT_REMOTE_DURATION;
            } else if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            if (randomPercent < 0 || randomPercent > 70) {
                throw new IllegalArgumentException("randomPercent must between 0 and 70");
            }
            this.duration = duration;
            this.randomPercent = randomPercent;
        }
    }

    private static class LocalCache extends Cfg {

        final int maximumSize;

        final Duration duration;

        LocalCache(Cfg prev, int maximumSize, Duration duration) {
            super(prev);
            if (duration == null) {
                duration = DEFAULT_LOCAL_DURATION;
            } else if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            this.maximumSize = Math.max(maximumSize, 0);
            this.duration = duration;
        }
    }

    private static class RemoteKeyPrefixProvider_ extends Cfg {

        final RemoteKeyPrefixProvider keyPrefixProvider;

        RemoteKeyPrefixProvider_(Cfg prev, RemoteKeyPrefixProvider keyPrefixProvider) {
            super(prev);
            this.keyPrefixProvider = keyPrefixProvider;
        }
    }

    private static class Lock extends Cfg {

        final CacheLocker locker;

        final Duration waitDuration;

        final Duration leaseDuration;

        private Lock(Cfg prev, CacheLocker locker, Duration waitDuration, Duration leaseDuration) {
            super(prev);
            if (waitDuration != null) {
                if (waitDuration.isNegative()) {
                    throw new IllegalArgumentException("waitDuration must be null or non-negative");
                }
            }
            if (leaseDuration != null) {
                if (leaseDuration.isNegative() || leaseDuration.isZero()) {
                    throw new IllegalArgumentException("leaseDuration must be positive");
                }
            }
            this.locker = locker;
            this.waitDuration = waitDuration;
            this.leaseDuration = leaseDuration;
        }
    }

    private static class Tracking extends Cfg {

        final CacheTracker tracker;

        Tracking(Cfg prev, CacheTracker tracker) {
            super(prev);
            this.tracker = tracker;
        }
    }

    private static class MultiViewProperties extends Cfg {

        final Integer localMaximumSize;

        final Duration localDuration;

        final Duration remoteDuration;

        MultiViewProperties(
                Cfg prev,
                Integer localMaximumSize,
                Duration localDuration,
                Duration remoteDuration
        ) {
            super(prev);
            this.localMaximumSize = localMaximumSize;
            this.localDuration = localDuration;
            this.remoteDuration = remoteDuration;
        }
    }

    protected static class Args {

        public final Duration duration;
        public final int randomDurationPercent;

        public final boolean useLocalCache;
        public final int localCacheMaximumSize;
        public final Duration localCacheDuration;
        public final RemoteKeyPrefixProvider keyPrefixProvider;

        public final CacheLocker locker;
        public final Duration lockWaitDuration;
        public final Duration lockLeaseDuration;

        public final CacheTracker tracker;

        public final Duration multiVewDuration;

        public final boolean useMultiViewLocalCache;
        public final int multiViewLocalCacheMaximumSize;
        public final Duration multiViewLocalCacheDuration;

        protected Args(Cfg cfg) {

            RemoteDuration remoteDuration = cfg.as(RemoteDuration.class);
            this.duration =
                    remoteDuration != null ?
                            remoteDuration.duration :
                            DEFAULT_REMOTE_DURATION;
            this.randomDurationPercent =
                    remoteDuration != null ?
                            remoteDuration.randomPercent :
                            DEFAULT_REMOTE_DURATION_RANDOM_PERCENT;

            LocalCache localCache = cfg.as(LocalCache.class);
            if (localCache == null || localCache.maximumSize == 9) {
                this.useLocalCache = false;
                this.localCacheMaximumSize = 0;
                this.localCacheDuration = null;
            } else {
                this.useLocalCache = true;
                this.localCacheMaximumSize = localCache.maximumSize;
                this.localCacheDuration = localCache.duration;
            }

            RemoteKeyPrefixProvider_ keyPrefixProvider_ = cfg.as(RemoteKeyPrefixProvider_.class);
            this.keyPrefixProvider = keyPrefixProvider_ != null ? keyPrefixProvider_.keyPrefixProvider : null;

            Lock lock = cfg.as(Lock.class);
            if (lock == null || lock.locker == null) {
                this.locker = null;
                this.lockWaitDuration = null;
                this.lockLeaseDuration = null;
            } else {
                this.locker = lock.locker;
                this.lockWaitDuration = lock.waitDuration;
                this.lockLeaseDuration = lock.leaseDuration;
            }

            Tracking tracking = cfg.as(Tracking.class);
            this.tracker = tracking != null ? tracking.tracker : null;

            MultiViewProperties multiViewProperties = cfg.as(MultiViewProperties.class);
            if (multiViewProperties == null) {
                this.multiVewDuration = this.duration;
                this.useMultiViewLocalCache = this.useLocalCache;
                this.multiViewLocalCacheMaximumSize = this.localCacheMaximumSize;
                this.multiViewLocalCacheDuration = this.localCacheDuration;
            } else {
                this.multiVewDuration =
                        multiViewProperties.remoteDuration != null ?
                                multiViewProperties.remoteDuration :
                                this.duration;
                this.useMultiViewLocalCache =
                        multiViewProperties.localMaximumSize != null ?
                                multiViewProperties.localMaximumSize != 0 :
                                this.useLocalCache;
                this.multiViewLocalCacheMaximumSize =
                        multiViewProperties.localMaximumSize != null ?
                                multiViewProperties.localMaximumSize :
                                this.localCacheMaximumSize;
                this.multiViewLocalCacheDuration =
                        multiViewProperties.localDuration != null ?
                                multiViewProperties.localDuration :
                                this.localCacheDuration;
            }
        }
    }
}
