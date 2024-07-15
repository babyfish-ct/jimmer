package org.babyfish.jimmer.sql.cache.redis.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheCreator;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineHashBinder;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineValueBinder;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Objects;

public class RedisCacheCreator implements CacheCreator {

    private final Cfg cfg;

    private Args args;

    public RedisCacheCreator(
            RedisConnectionFactory connectionFactory
    ) {
        this(connectionFactory, null);
    }

    public RedisCacheCreator(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        this.cfg = new Root(connectionFactory, objectMapper);
    }

    private RedisCacheCreator(Cfg cfg) {
        this.cfg = cfg;
    }

    @NotNull
    public CacheCreator withRemoteDuration(@Nullable Duration duration, int randomPercent) {
        return new RedisCacheCreator(
                new RedisDuration(cfg, duration, randomPercent)
        );
    }

    @NotNull
    public CacheCreator withLocalCache(int maxSize, Duration duration) {
        return new RedisCacheCreator(
                new LocalCache(cfg, maxSize, duration)
        );
    }

    @NotNull
    public CacheCreator withLock(
            @Nullable CacheLocker locker,
            @Nullable Duration waitDuration,
            @Nullable Duration leaseDuration
    ) {
        return new RedisCacheCreator(
                new Lock(cfg, locker, waitDuration, leaseDuration)
        );
    }

    @Override
    public @NotNull CacheCreator withTracking(@Nullable CacheTracker tracker) {
        return new RedisCacheCreator(
                new Tracking(cfg, tracker)
        );
    }

    @Override
    public <K, V> Cache<K, V> createForObject(ImmutableType type) {
        return new ChainCacheBuilder<K, V>()
                .add(caffeineValueBinder(type))
                .add(redisValueBinder(type))
                .build();
    }

    @Override
    public <K, V> Cache<K, V> createForProp(ImmutableProp prop, boolean multiView) {
        if (!prop.isReference(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot create associated cache for \"" +
                            prop +
                            "\" because it is not association reference property"
            );
        }
        if (multiView) {
            return new ChainCacheBuilder<K, V>()
                    .add(caffeineHashBinder(prop))
                    .add(redisHashBinder(prop))
                    .build();
        }
        return new ChainCacheBuilder<K, V>()
                .add(caffeineValueBinder(prop))
                .add(redisValueBinder(prop))
                .build();
    }

    private Args args() {
        Args args = this.args;
        if (args == null) {
            this.args = args = new Args(cfg);
        }
        return args;
    }

    private <K, V> LoadingBinder<K, V> caffeineValueBinder(ImmutableType type) {
        Args args = args();
        if (!args.useLocalCache) {
            return null;
        }
        return CaffeineValueBinder
                .<K, V>forObject(type)
                .subscribe(args.tracker)
                .maximumSize(args.localCacheMaxSize)
                .duration(args.localCacheDuration)
                .build();
    }

    private <K, V> LoadingBinder<K, V> caffeineValueBinder(ImmutableProp prop) {
        Args args = args();
        if (!args.useLocalCache) {
            return null;
        }
        return CaffeineValueBinder
                .<K, V>forProp(prop)
                .subscribe(args.tracker)
                .maximumSize(args.localCacheMaxSize)
                .duration(args.localCacheDuration)
                .build();
    }

    private <K, V> SimpleBinder<K, V> caffeineHashBinder(ImmutableProp prop) {
        Args args = args();
        if (!args.useLocalCache) {
            return null;
        }
        return CaffeineHashBinder
                .<K, V>forProp(prop)
                .subscribe(args.tracker)
                .maximumSize(args.localCacheMaxSize)
                .duration(args.localCacheDuration)
                .build();
    }

    private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableType type) {
        Args args = args();
        return RedisValueBinder
                .<K, V>forObject(type)
                .publish(args.tracker)
                .duration(args.duration)
                .objectMapper(args.objectMapper)
                .duration(args.duration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.connectionFactory)
                .build()
                .lock(
                        args.locker,
                        args.lockWaitDuration,
                        args.lockLeaseDuration
                );
    }

    private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableProp prop) {
        Args args = args();
        return RedisValueBinder
                .<K, V>forProp(prop)
                .publish(args.tracker)
                .duration(args.duration)
                .objectMapper(args.objectMapper)
                .duration(args.duration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.connectionFactory)
                .build()
                .lock(
                        args.locker,
                        args.lockWaitDuration,
                        args.lockLeaseDuration
                );
    }

    private <K, V> SimpleBinder.Parameterized<K, V> redisHashBinder(ImmutableProp prop) {
        Args args = args();
        return RedisHashBinder
                .<K, V>forProp(prop)
                .publish(args.tracker)
                .duration(args.duration)
                .objectMapper(args.objectMapper)
                .duration(args.duration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.connectionFactory)
                .build()
                .lock(
                        args.locker,
                        args.lockWaitDuration,
                        args.lockLeaseDuration
                );
    }

    private static abstract class Cfg {

        final Cfg prev;

        Cfg(Cfg prev) {
            this.prev = prev;
        }

        @SuppressWarnings("unchecked")
        <T extends Cfg> T as(Class<T> type) {
            if (this.getClass() == type) {
                return (T)this;
            }
            if (prev != null) {
                return prev.as(type);
            }
            return null;
        }
    }

    private static class Root extends Cfg {

        final RedisConnectionFactory connectionFactory;

        final ObjectMapper objectMapper;

        private Root(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
            super(null);
            this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory cannot be null");
            this.objectMapper = objectMapper;
        }
    }

    private static class RedisDuration extends Cfg {

        final Duration duration;

        int randomPercent;

        RedisDuration(Cfg prev, Duration duration, int randomPercent) {
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

        final int maxSize;

        final Duration duration;

        LocalCache(Cfg prev, int maxSize, Duration duration) {
            super(prev);
            if (duration == null) {
                duration = DEFAULT_LOCAL_DURATION;
            } else if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            this.maxSize = Math.max(maxSize, 0);
            this.duration = duration;
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

    static class Args {

        final RedisConnectionFactory connectionFactory;
        final ObjectMapper objectMapper;

        final Duration duration;
        final int randomDurationPercent;

        final boolean useLocalCache;
        final int localCacheMaxSize;
        final Duration localCacheDuration;

        final CacheLocker locker;
        final Duration lockWaitDuration;
        final Duration lockLeaseDuration;

        final CacheTracker tracker;

        Args(Cfg cfg) {

            Root root = cfg.as(Root.class);

            this.connectionFactory = root.connectionFactory;
            ObjectMapper mapper = root.objectMapper;
            ObjectMapper clonedMapper = mapper != null ?
                    new ObjectMapper(mapper) {} :
                    new ObjectMapper();
            clonedMapper.registerModule(new JavaTimeModule());
            clonedMapper.registerModule(new ImmutableModule());
            this.objectMapper = clonedMapper;

            RedisDuration redisDuration = cfg.as(RedisDuration.class);
            this.duration =
                    redisDuration != null ?
                            redisDuration.duration :
                            DEFAULT_REMOTE_DURATION;
            this.randomDurationPercent =
                    redisDuration != null ?
                            redisDuration.randomPercent :
                            DEFAULT_REMOTE_DURATION_RANDOM_PERCENT;

            LocalCache localCache = cfg.as(LocalCache.class);
            if (localCache == null || localCache.maxSize == 9) {
                this.useLocalCache = false;
                this.localCacheMaxSize = 0;
                this.localCacheDuration = null;
            } else {
                this.useLocalCache = true;
                this.localCacheMaxSize = localCache.maxSize;
                this.localCacheDuration = localCache.duration;
            }

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
        }
    }
}

