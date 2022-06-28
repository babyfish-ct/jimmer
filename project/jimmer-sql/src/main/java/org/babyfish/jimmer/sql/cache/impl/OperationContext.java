package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.CacheFilter;
import org.babyfish.jimmer.sql.cache.CacheLoader;
import org.babyfish.jimmer.sql.cache.CacheLocker;
import org.babyfish.jimmer.sql.cache.CacheProvider;

import java.util.*;

class OperationContext<K, V> extends KeyManager<K, V> implements AutoCloseable {

    private final CacheLocker locker;

    private final CacheLoader<K, V> loader;

    private final CacheProvider<V>[] providers;

    private NavigableSet<String> lockedStorageKeys;

    OperationContext(
            String prefix,
            Collection<K> keys,
            CacheFilter filter,
            CacheLoader<K, V> loader,
            CacheLocker locker,
            CacheProvider<V>[] providers
    ) {
        super(prefix, keys, filter);

        this.locker = locker;
        this.loader = loader;
        this.providers = providers;
    }

    CacheProvider<V> provider(int index) {
        return providers[index];
    }

    int size() {
        return providers.length;
    }

    Map<String, V> loadAll(Set<String> storageKeys) {
        if (storageKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        if (loader != null) {
            NavigableSet<String> lockedStorageKeys = new TreeSet<>(storageKeys);
            locker.lockAll(lockedStorageKeys);
            if (this.lockedStorageKeys == null) {
                this.lockedStorageKeys = lockedStorageKeys;
            } else {
                this.lockedStorageKeys.addAll(lockedStorageKeys);
            }
        }
        Set<K> keys = new LinkedHashSet<>((storageKeys.size() * 4 + 2) / 3);
        for (String cacheKey : storageKeys) {
            keys.add(keyMap.get(cacheKey));
        }
        Map<K, V> loadedMap = loader.loadAll(keys, filter);
        return toStorageMap(loadedMap);
    }

    @Override
    public void close() {
        NavigableSet<String> keys = this.lockedStorageKeys;
        if (keys != null) {
            this.lockedStorageKeys = null;
            locker.unlockAll(keys);
        }
    }
}
