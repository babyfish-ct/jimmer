package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.sql.SqlClient;

import java.util.*;

public class ObjectCache<T> implements Cache<Object, T> {

    private final Class<T> type;

    private Map<Object, T> map = new HashMap<>();

    public ObjectCache(Class<T> type) {
        this.type = type;
    }

    @Override
    public Map<Object, T> getAll(Collection<Object> keys, CacheEnvironment env) {
        if (!CacheFilter.isEmpty(env.getFilter())) {
            throw new IllegalArgumentException("Object cache does not support filter");
        }
        Map<Object, T> resultMap = new LinkedHashMap<>();
        Set<Object> missedKeys = new LinkedHashSet<>();
        for (Object key : keys) {
            T value = (T) map.get(key);
            resultMap.put(key, value);
            if (value == null && !map.containsKey(key)) {
                missedKeys.add(key);
            }
        }
        Map<Object, T> loadedMap = env
                .getSqlClient()
                .getEntities()
                .findMapByIds(type, missedKeys, env.getConnection());
        for (Map.Entry<Object, T> e : resultMap.entrySet()) {
            if (e.getValue() == null) {
                e.setValue(loadedMap.get(e.getKey()));
            }
        }
        map.putAll(loadedMap);
        return resultMap;
    }

    @Override
    public void deleteAll(Collection<Object> keys, CacheEnvironment env) {
        map.keySet().removeAll(keys);
    }
}
