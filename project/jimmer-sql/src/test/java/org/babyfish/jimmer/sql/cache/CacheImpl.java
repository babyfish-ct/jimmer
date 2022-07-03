package org.babyfish.jimmer.sql.cache;

import java.util.*;

public class CacheImpl<T> implements Cache<Object, T> {

    private Map<Object, T> map = new HashMap<>();

    @Override
    public Map<Object, T> getAll(Collection<Object> keys, QueryCacheEnvironment<Object, T> env) {
        if (!CacheFilter.isEmpty(env.getFilter())) {
            throw new IllegalArgumentException("Object cache does not support filter");
        }
        Map<Object, T> resultMap = new LinkedHashMap<>();
        Set<Object> missedKeys = new LinkedHashSet<>();
        for (Object key : keys) {
            T value = map.get(key);
            resultMap.put(key, value);
            if (value == null && !map.containsKey(key)) {
                missedKeys.add(key);
            }
        }
        Map<Object, T> loadedMap = env
                .getLoader()
                .loadAll(missedKeys);
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
