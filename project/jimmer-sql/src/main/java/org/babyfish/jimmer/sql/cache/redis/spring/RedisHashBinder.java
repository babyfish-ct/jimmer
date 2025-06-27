package org.babyfish.jimmer.sql.cache.redis.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteHashBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * framework-related classes should not be included in the jimmer-sql module.<br>
 * <br>
 * Redis-related caching should be implemented through framework-specific extensions.
 * @see "org.babyfish.jimmer.spring.cache.RedisHashBinder(Provided by jimmer-spring-boot-starter)"
 */
@Deprecated
public class RedisHashBinder<K, V> extends AbstractRemoteHashBinder<K, V> {

    private final RedisOperations<String, byte[]> operations;

    protected RedisHashBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @Nullable ObjectMapper objectMapper,
            @NotNull Duration duration,
            int randomPercent,
            @NotNull RedisOperations<String, byte[]> operations
    ) {
        super(
                type,
                prop,
                tracker,
                objectMapper,
                duration,
                randomPercent
        );
        this.operations = operations;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<byte[]> read(Collection<String> keys, String hashKey) {
        return (List<byte[]>)(List<?>)operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        for (String key : keys) {
                            pipelinedOps.opsForHash().get(key, hashKey);
                        }
                        return null;
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void write(Map<String, byte[]> map, String hashKey) {
        operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        for (Map.Entry<String, byte[]> e : map.entrySet()) {
                            pipelinedOps.opsForHash().put(e.getKey(), hashKey, e.getValue());
                            pipelinedOps.expire(
                                    e.getKey(),
                                    nextExpireMillis(),
                                    TimeUnit.MILLISECONDS
                            );
                        }
                        return null;
                    }
                }
        );
    }

    @Override
    protected void deleteAllSerializedKeys(List<String> serializedKeys) {
        operations.delete(serializedKeys);
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

        private RedisOperations<String, byte[]> operations;

        protected Builder(ImmutableType type, ImmutableProp prop) {
            super(type, prop);
        }

        public Builder<K, V> redis(RedisOperations<String, byte[]> operations) {
            this.operations = operations;
            return this;
        }

        public Builder<K, V> redis(RedisConnectionFactory connectionFactory) {
            this.operations = RedisCaches.cacheRedisTemplate(connectionFactory);
            return this;
        }

        public RedisHashBinder<K, V> build() {
            if (operations == null) {
                throw new IllegalStateException(
                        "Redis operations or redis connection factory has not been specified"
                );
            }
            return new RedisHashBinder<>(
                    type,
                    prop,
                    tracker,
                    objectMapper,
                    duration,
                    randomPercent,
                    operations
            );
        }
    }
}
