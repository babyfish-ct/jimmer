package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.sql.cache.CacheChain;
import org.babyfish.jimmer.sql.cache.CacheProvider;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSimpleCacheProvider<V> implements CacheProvider<V> {

    @Override
    public final Map<String, V> getAll(Set<String> storageKeys, CacheChain<V> chain) {

        Map<String, V> cachedMap = read(storageKeys);

        Set<String> missedStorageKeys = new LinkedHashSet<>();
        for (String storageKey : storageKeys) {
            if (!cachedMap.containsKey(storageKey)) {
                missedStorageKeys.add(storageKey);
            }
        }
        if (missedStorageKeys.isEmpty()) {
            return cachedMap;
        }

        Map<String, V> moreMap = chain.getAll(missedStorageKeys);

        Map<String, V> writeMap = new LinkedHashMap<>((moreMap.size() * 4 + 2) / 3);
        for (String missedStorageKey : missedStorageKeys) {
            V value = moreMap.get(missedStorageKey);
            if (value != null || isNullSavable()) {
                writeMap.put(missedStorageKey, value);
            }
        }
        write(writeMap);

        // Keep result order
        Map<String, V> resultMap = new LinkedHashMap<>(
                ((cachedMap.size() + moreMap.size()) * 4 + 2) / 3
        );
        for (String storageKey : storageKeys) {
            V value = cachedMap.get(storageKey);
            if (value == null) {
                value = moreMap.get(storageKey);
            }
            if (value != null || cachedMap.containsKey(storageKey) || moreMap.containsKey(storageKey)) {
                resultMap.put(storageKey, value);
            }
        }
        return resultMap;
    }

    protected abstract Map<String, V> read(Set<String> storageKeys);

    protected abstract void write(Map<String, V> map);

    protected abstract boolean isNullSavable();
}
