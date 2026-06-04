package org.babyfish.jimmer.sql.cache.redis.quarkus;

import io.quarkus.redis.datasource.RedisDataSource;
import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.Cache;
import org.babyfish.jimmer.sql.cache.CacheCreator;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineHashBinder;
import org.babyfish.jimmer.sql.cache.caffeine.CaffeineValueBinder;
import org.babyfish.jimmer.sql.cache.chain.ChainCacheBuilder;
import org.babyfish.jimmer.sql.cache.chain.LoadingBinder;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.babyfish.jimmer.sql.cache.spi.AbstractCacheCreator;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * framework-related classes should not be included in the jimmer-sql module.<br>
 * <br>
 * Redis-related caching should be implemented through framework-specific extensions.
 *
 * @see "io.quarkiverse.jimmer.runtime.cache.RedisCacheCreator(Provided by https://github.com/flynndi/quarkus-jimmer-extension)"
 */
@Deprecated
public class RedisCacheCreator extends AbstractCacheCreator {

    public RedisCacheCreator(RedisDataSource redisDataSource) {
        this(redisDataSource, JsonCodec.jsonCodec());
    }

    public RedisCacheCreator(RedisDataSource redisDataSource, @NotNull JsonCodec<?> jsonCodec) {
        super(new Root(redisDataSource, jsonCodec));
    }

    protected RedisCacheCreator(Cfg cfg) {
        super(cfg);
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

    @Override
    protected Args newArgs(Cfg cfg) {
        return new Args(cfg);
    }

    @Override
    protected CacheCreator newCacheCreator(Cfg cfg) {
        return new RedisCacheCreator(cfg);
    }

    private <K, V> LoadingBinder<K, V> caffeineValueBinder(ImmutableType type) {
        Args args = args();
        if (!args.useLocalCache) {
            return null;
        }
        return CaffeineValueBinder
                .<K, V>forObject(type)
                .subscribe(args.tracker)
                .maximumSize(args.localCacheMaximumSize)
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
                .maximumSize(args.localCacheMaximumSize)
                .duration(args.localCacheDuration)
                .build();
    }

    private <K, V> SimpleBinder<K, V> caffeineHashBinder(ImmutableProp prop) {
        Args args = args();
        if (!args.useMultiViewLocalCache) {
            return null;
        }
        return CaffeineHashBinder
                .<K, V>forProp(prop)
                .subscribe(args.tracker)
                .maximumSize(args.multiViewLocalCacheMaximumSize)
                .duration(args.multiViewLocalCacheDuration)
                .build();
    }

    private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableType type) {
        Args args = args();
        return RedisValueBinder
                .<K, V>forObject(type, args.jsonCodec)
                .publish(args.tracker)
                .keyPrefixProvider(args.keyPrefixProvider)
                .duration(args.duration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.redisDataSource)
                .build()
                .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
    }

    private <K, V> SimpleBinder<K, V> redisValueBinder(ImmutableProp prop) {
        Args args = args();
        return RedisValueBinder
                .<K, V>forProp(prop, args.jsonCodec)
                .publish(args.tracker)
                .keyPrefixProvider(args.keyPrefixProvider)
                .duration(args.duration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.redisDataSource)
                .build()
                .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
    }

    private <K, V> SimpleBinder.Parameterized<K, V> redisHashBinder(ImmutableProp prop) {
        Args args = args();
        return RedisHashBinder
                .<K, V>forProp(prop, args.jsonCodec)
                .publish(args.tracker)
                .keyPrefixProvider(args.keyPrefixProvider)
                .duration(args.multiVewDuration)
                .randomPercent(args.randomDurationPercent)
                .redis(args.redisDataSource)
                .build()
                .lock(args.locker, args.lockWaitDuration, args.lockLeaseDuration);
    }

    private static class Root extends Cfg {

        final RedisDataSource redisDataSource;

        final JsonCodec<?> jsonCodec;

        private Root(RedisDataSource redisDataSource, @NotNull JsonCodec<?> jsonCodec) {
            super(null);
            this.redisDataSource = Objects.requireNonNull(redisDataSource, "redisDataSource cannot be null");
            this.jsonCodec = jsonCodec;
        }
    }

    static class Args extends AbstractCacheCreator.Args {

        final RedisDataSource redisDataSource;

        final JsonCodec<?> jsonCodec;

        Args(Cfg cfg) {
            super(cfg);
            Root root = cfg.as(Root.class);
            this.redisDataSource = root.redisDataSource;
            this.jsonCodec = root.jsonCodec;
        }
    }
}
