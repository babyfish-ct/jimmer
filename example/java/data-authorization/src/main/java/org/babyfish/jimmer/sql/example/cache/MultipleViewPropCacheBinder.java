package org.babyfish.jimmer.sql.example.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.cache.chain.SimpleBinder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultipleViewPropCacheBinder implements SimpleBinder.Parameterized<Object, Object> {

    private final ConcurrentMap<String, ConcurrentMap<String, Object>> dataMap;

    private final ImmutableProp prop;

    public MultipleViewPropCacheBinder(ConcurrentMap<String, ConcurrentMap<String, Object>> dataMap, ImmutableProp prop) {
        this.dataMap = dataMap;
        this.prop = prop;
    }

    @Override
    public Map<Object, Object> getAll(Collection<Object> keys, SortedMap<String, Object> parameterMap) {
        String subKey = parameterMap.toString();
        Map<Object, Object> resultMap = new HashMap<>();
        for (Object key : keys) {
            ConcurrentMap<String, Object> subMap = dataMap.get(toDataKey(key));
            if (subMap != null) {
                Object result = subMap.get(subKey);
                if (result != null) {
                    resultMap.put(key, result);
                }
            }
        }
        return resultMap;
    }

    @Override
    public void setAll(Map<Object, Object> map, SortedMap<String, Object> parameterMap) {
        String subKey = parameterMap.toString();
        for (Map.Entry<Object, Object> e : map.entrySet()) {
            String key = toDataKey(e.getKey());
            Object value = e.getValue();
            dataMap
                    .computeIfAbsent(key, it -> new ConcurrentHashMap<>())
                    .put(subKey, value);
        }
    }

    @Override
    public void deleteAll(Collection<Object> keys, Object reason) {
        for (Object key : keys) {
            String dataKey = toDataKey(key);
            System.out.println("------------------------------------------------");
            System.out.println("Delete multi-view prop cache: " + dataKey);
            System.out.println("------------------------------------------------");
            dataMap.remove(dataKey);
        }
    }

    private String toDataKey(Object key) {
        return prop.getDeclaringType().getJavaClass().getSimpleName() +
                '.' +
                prop.getName() +
                '-' +
                key;
    }
}