package org.babyfish.jimmer.sql.example.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisBinder<K, V> implements SimpleBinder<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisBinder.class);

    private static final byte[] NULL_BYTES = "<null>".getBytes(StandardCharsets.UTF_8);

    private static final ObjectMapper IMMUTABLE_MAPPER = new ObjectMapper()
            .registerModule(new ImmutableModule());

    private final RedisOperations<String, byte[]> operations;

    private final String keyPrefix;

    private final Duration duration;

    private final int randomPercent;

    private final JavaType valueType;

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
        if (prop != null) {
            JavaType targetIdType = SimpleType.constructUnsafe(
                    prop.getTargetType().getIdProp().getElementClass()
            );
            if (prop.isEntityList()) {
                this.valueType = CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        targetIdType
                );
            } else {
                this.valueType = targetIdType;
            }
        } else {
            this.valueType = SimpleType.constructUnsafe(type.getJavaClass());
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
                byte[] value = valueItr.next();
                if (value != null) {
                    if (Arrays.equals(value, NULL_BYTES)) {
                        map.put(key, null);
                    } else {
                        try {
                            map.put(key, IMMUTABLE_MAPPER.readValue(value, valueType));
                        } catch (IOException ex) {
                            throw new IllegalArgumentException(ex);
                        }
                    }
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAll(Map<K, V> map) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Save {} into redis", map);
        }
        Map<String, byte[]> convertedMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, V> e : map.entrySet()) {
            V value = e.getValue();
            if (value == null) {
                convertedMap.put(keyPrefix + e.getKey(), NULL_BYTES);
            } else {
                try {
                    convertedMap.put(keyPrefix + e.getKey(), IMMUTABLE_MAPPER.writeValueAsBytes(value));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }
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
    public void deleteAll(Collection<K> keys) {
        operations.delete(
                keys.stream().map(it -> keyPrefix + it).collect(Collectors.toList())
        );
    }
}
