package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.ValueSerializer;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// Level-2 Cache
public class RedisBinder<K, V> implements SimpleBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBinder.class);

    private final RedisOperations<String, byte[]> operations;

    private final String keyPrefix;

    private final Duration duration;

    private final int randomPercent;

    private final ValueSerializer<V> valueSerializer;

    public RedisBinder(
            RedisOperations<String, byte[]> operations,
            ImmutableType type,
            Duration duration
    ) {
        this(operations, type, null, duration, 30);
    }

    public RedisBinder(
            RedisOperations<String, byte[]> operations,
            ImmutableProp prop,
            Duration duration
    ) {
        this(operations, null, prop, duration, 30);
    }

    private RedisBinder(
            RedisOperations<String, byte[]> operations,
            ImmutableType type,
            ImmutableProp prop,
            Duration duration,
            int randomPercent
    ) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("Illegal metadata");
        }
        if (randomPercent < 0 || randomPercent > 100) {
            throw new IllegalArgumentException("randomPercent must between 0 and 100");
        }
        this.operations = operations;
        if (type != null) {
            this.keyPrefix = type.getJavaClass().getSimpleName() + '-';
        } else {
            this.keyPrefix = prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
        }
        this.duration = duration;
        this.randomPercent = randomPercent;
        if (type != null) {
            valueSerializer = new ValueSerializer<>(type);
        } else {
            valueSerializer = new ValueSerializer<>(prop);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        List<byte[]> values = operations.opsForValue().multiGet(
                keys.stream().map(it -> keyPrefix + it).collect(Collectors.toList())
        );
        Map<K, V> map = new HashMap<>((keys.size() * 4 + 2) / 3);
        if (values != null) {
            Iterator<K> keyItr = keys.iterator();
            Iterator<byte[]> valueItr = values.iterator();
            while (keyItr.hasNext() && valueItr.hasNext()) {
                K key = keyItr.next();
                byte[] bytes = valueItr.next();
                if (bytes != null) {
                    V value = valueSerializer.deserialize(bytes);
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAll(Map<K, V> map) {
        Map<String, byte[]> convertedMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, V> e : map.entrySet()) {
            convertedMap.put(keyPrefix + e.getKey(), valueSerializer.serialize(e.getValue()));
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Save into redis: {}",
                    convertedMap
                            .entrySet()
                            .stream()
                            .map(it -> it.getKey() + ":" + new String(it.getValue()))
                            .collect(Collectors.joining(", "))
            );
        }
        long millis = duration.toMillis();
        long min = millis - randomPercent * millis / 100;
        long max = millis + randomPercent * millis / 100;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        operations.executePipelined(
                new SessionCallback<Void>() {
                    @Override
                    public <XK, XV> Void execute(RedisOperations<XK, XV> pops) throws DataAccessException {
                        RedisOperations<String, byte[]> pipelinedOps = (RedisOperations<String, byte[]>)pops;
                        pipelinedOps.opsForValue().multiSet(convertedMap);
                        for (String key : convertedMap.keySet()) {
                            pipelinedOps.expire(
                                    key,
                                    random.nextLong(min, max),
                                    TimeUnit.MILLISECONDS
                            );
                        }
                        return null;
                    }
                }
        );
    }

    @Override
    public void deleteAll(Collection<K> keys, String reason) {
        if (reason == null || reason.equals("redis")) {
            Collection<String> redisKeys = keys.stream().map(it -> keyPrefix + it).collect(Collectors.toList());
            LOGGER.info("delete from redis: {}", redisKeys);
            operations.delete(redisKeys);
        }
    }
}
