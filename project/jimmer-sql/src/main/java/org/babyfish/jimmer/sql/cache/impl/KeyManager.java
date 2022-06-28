package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.CacheFilter;

import java.util.*;

class KeyManager<K, V> {

    private final String prefix;

    private final String suffix;

    protected final Map<String, K> keyMap;

    protected final Map<K, String> storageKeyMap;

    protected CacheFilter filter;

    public KeyManager(
            String prefix,
            Collection<K> keys,
            CacheFilter filter
    ) {
        String suffix;
        Map<String, Object> args = null;
        if (filter != null) {
            args = filter.toCacheArgs();
        }
        if (args == null || args.isEmpty()) {
            suffix = null;
        } else {
            suffix = ":" + args.toString();
        }

        Map<String, K> keyMap = new LinkedHashMap<>();
        Map<K, String> storageKeyMap = new LinkedHashMap<>();
        for (K key : keys) {
            String storageKey = key.toString();
            if (prefix != null) {
                storageKey = prefix + storageKey;
            }
            if (suffix != null) {
                storageKey = storageKey + suffix;
            }
            keyMap.put(storageKey, key);
            storageKeyMap.put(key, storageKey);
        }

        this.prefix = prefix;
        this.suffix = suffix;
        this.keyMap = Collections.unmodifiableMap(keyMap);
        this.storageKeyMap = Collections.unmodifiableMap(storageKeyMap);
        this.filter = filter;
    }

    Set<K> keys() {
        return storageKeyMap.keySet();
    }

    Set<String> storageKeys() {
        return keyMap.keySet();
    }

    public Map<K, V> toMap(Map<String, V> map) {
        Map<K, V> resultMap = new LinkedHashMap<>((map.size() + 4 + 2) / 3);
        for (Map.Entry<String, V> e : map.entrySet()) {
            K key = keyMap.get(e.getKey());
            V value = e.getValue();
            if (value != null) {
                resultMap.put(key, value);
            }
        }
        return resultMap;
    }

    public Map<String, V> toStorageMap(Map<K, V> map) {
        Map<String, V> resultMap = new LinkedHashMap<>((map.size() + 4 + 2) / 3);
        for (Map.Entry<K, V> e : map.entrySet()) {
            String storageKey = storageKeyMap.get(e.getKey());
            V value = e.getValue();
            if (value != null) {
                resultMap.put(storageKey, value);
            }
        }
        return resultMap;
    }
}
