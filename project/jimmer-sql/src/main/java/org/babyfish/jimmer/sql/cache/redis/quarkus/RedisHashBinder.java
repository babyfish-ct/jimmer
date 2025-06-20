package org.babyfish.jimmer.sql.cache.redis.quarkus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;

/**
 * framework-related classes should not be included in the jimmer-sql module.<br>
 * <br>
 * Redis-related caching should be implemented through framework-specific extensions.
 * @see io.quarkiverse.jimmer.runtime.cache.RedisCacheCreator
 */
@Deprecated
public class RedisHashBinder<K, V> extends AbstractRemoteHashBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisHashBinder.class);

    private final HashCommands<String, String, byte[]> hashCommands;

    private final ValueCommands<String, byte[]> valueCommands;

    protected RedisHashBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @Nullable ObjectMapper objectMapper,
            @NotNull Duration duration,
            int randomPercent,
            @NotNull RedisDataSource redisDataSource) {
        super(type, prop, tracker, objectMapper, duration, randomPercent);
        this.hashCommands = redisDataSource.hash(byte[].class);
        this.valueCommands = redisDataSource.value(byte[].class);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<byte[]> read(Collection<String> keys, String hashKey) {
        if (keys.isEmpty()) {
            return null;
        }
        List<byte[]> list = new ArrayList<>();
        for (String key : keys) {
            list.add(hashCommands.hget(key, hashKey));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void write(Map<String, byte[]> map, String hashKey) {
        for (Map.Entry<String, byte[]> e : map.entrySet()) {
            hashCommands.hset(e.getKey(), hashKey, e.getValue());
            for (String key : map.keySet()) {
                valueCommands.getex(key, new GetExArgs().px(nextExpireMillis()));
            }
        }
    }

    @Override
    protected void deleteAllSerializedKeys(List<String> serializedKeys) {
        for (String key : serializedKeys) {
            valueCommands.getdel(key);
        }
    }

    @Override
    protected boolean matched(@Nullable Object reason) {
        return "redis".equals(reason);
    }

    @NotNull
    public static <K, V> Builder<K, V> forProp(ImmutableProp prop) {
        return new Builder<>(null, prop);
    }

    public static class Builder<K, V> extends AbstractBuilder<K, V, Builder<K, V>> {

        private RedisDataSource redisDataSource;

        protected Builder(ImmutableType type, ImmutableProp prop) {
            super(type, prop);
        }

        public Builder<K, V> redis(RedisDataSource redisDataSource) {
            this.redisDataSource = redisDataSource;
            return this;
        }

        public RedisHashBinder<K, V> build() {
            if (null == redisDataSource) {
                throw new IllegalStateException("RedisDataSource has not been specified");
            }
            return new RedisHashBinder<>(type, prop, tracker, objectMapper, duration, randomPercent, redisDataSource);
        }
    }
}
