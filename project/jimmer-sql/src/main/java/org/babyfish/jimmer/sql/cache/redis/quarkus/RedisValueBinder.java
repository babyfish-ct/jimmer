package org.babyfish.jimmer.sql.cache.redis.quarkus;

import java.time.Duration;
import java.util.*;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.RemoteKeyPrefixProvider;
import org.babyfish.jimmer.sql.cache.spi.AbstractRemoteValueBinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ValueCommands;

/**
 * framework-related classes should not be included in the jimmer-sql module.<br>
 * <br>
 * Redis-related caching should be implemented through framework-specific extensions.
 * @see "io.quarkiverse.jimmer.runtime.cache.RedisCacheCreator(Provided by https://github.com/flynndi/quarkus-jimmer-extension)"
 */
@Deprecated
public class RedisValueBinder<K, V> extends AbstractRemoteValueBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisValueBinder.class);

    private final ValueCommands<String, byte[]> operations;

    protected RedisValueBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop,
            @Nullable CacheTracker tracker,
            @Nullable ObjectMapper objectMapper,
            @Nullable RemoteKeyPrefixProvider keyPrefixProvider,
            @NotNull Duration duration,
            int randomPercent,
            @NotNull RedisDataSource redisDataSource) {
        super(type, prop, tracker, objectMapper, keyPrefixProvider, duration, randomPercent);
        this.operations = redisDataSource.value(byte[].class);
    }

    @Override
    protected List<byte[]> read(Collection<String> keys) {
        return this.multiGet(keys, operations);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void write(Map<String, byte[]> map) {
        operations.mset(map);
        for (String key : map.keySet()) {
            operations.getex(key, new GetExArgs().px(nextExpireMillis()));
        }
    }

    @Override
    protected void deleteAllSerializedKeys(List<String> serializedKeys) {
        for (String key : serializedKeys) {
            operations.getdel(key);
        }
    }

    @Override
    protected boolean matched(@Nullable Object reason) {
        return "redis".equals(reason);
    }

    private List<byte[]> multiGet(Collection<String> keys, ValueCommands<String, byte[]> operations) {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        String[] array = keys.toArray(keys.toArray(new String[0]));
        Map<String, byte[]> mGet = operations.mget(array);
        return new ArrayList<>(mGet.values());
    }

    @NotNull
    public static <K, V> Builder<K, V> forObject(ImmutableType type) {
        return new Builder<>(type, null);
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

        public RedisValueBinder<K, V> build() {
            if (null == redisDataSource) {
                throw new IllegalStateException("RedisDataSource has not been specified");
            }
            return new RedisValueBinder<>(type, prop, tracker, objectMapper, keyPrefixProvider, duration, randomPercent, redisDataSource);
        }
    }
}
