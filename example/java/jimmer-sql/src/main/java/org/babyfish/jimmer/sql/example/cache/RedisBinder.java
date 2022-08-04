package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.sql.example.cache.chain.SimpleBinder;
import org.springframework.data.redis.core.RedisOperations;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class RedisBinder<K, V> implements SimpleBinder<K, V> {

    private final RedisOperations<String, ?> operations;

    private final String keyPrefix;

    private final Duration duration;

    public RedisBinder(RedisOperations<String, ?> operations, String keyPrefix, Duration duration) {
        this.operations = operations;
        this.keyPrefix = keyPrefix;
        this.duration = duration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        List<V> values = (List<V>) operations.opsForValue().multiGet(
                keys.stream().map(it -> keyPrefix + it).collect(Collectors.toList())
        );
        Map<K, V> map = new HashMap<>((keys.size() * 4 + 2) / 3);
        if (values != null) {
            Iterator<K> keyItr = keys.iterator();
            Iterator<V> valueItr = values.iterator();
            while (keyItr.hasNext() && valueItr.hasNext()) {
                V value = valueItr.next();
                if (value != null) {
                    map.put(keyItr.next(), value);
                }
            }
        }
        return map;
    }

    @Override
    public void setAll(Map<K, V> map) {
        Map<String, V> convertedMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (Map.Entry<K, V> e : map.entrySet()) {
            convertedMap.put(keyPrefix + e.getKey(), e.getValue());
        }
        operations.opsForValue().multiSet((Map)convertedMap);

    }

    @Override
    public void deleteAll(Collection<K> keys) {
        operations.delete(
                keys.stream().map(it -> keyPrefix + it).collect(Collectors.toList())
        );
    }
}
