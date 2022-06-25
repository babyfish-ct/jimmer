package org.babyfish.jimmer.sql.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class CacheImpl<K, V> implements Cache<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheImpl.class);

    private final String keyPrefix;

    private final Collection<CacheImplementation<V>> implementations;

    private final CacheLoader<K, V> loader;

    private final CacheLocker locker;

    public CacheImpl(
            String keyPrefix,
            CacheBinder binder,
            CacheLoader<K, V> loader,
            CacheLocker locker
    ) {
        Objects.requireNonNull(keyPrefix, "keyPrefix cannot be null");
        Objects.requireNonNull(binder, "binder cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        this.keyPrefix = keyPrefix;
        this.implementations = CacheBinder.toImplementations(binder);
        this.loader = loader;
        this.locker = locker;
    }

    @Override
    public final V get(K key, CacheFilter filter) {
        return getAll(Collections.singleton(key), filter).get(key);
    }

    @Override
    public final Map<K, V> getAll(Collection<K> keys, CacheFilter filter) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        return new Getter(keys, filter).execute();
    }

    @Override
    public void delete(K key, CacheFilter filter) {
        deleteAll(Collections.singleton(key), filter);
    }

    @Override
    public void deleteAll(Collection<K> keys, CacheFilter filter) {
        if (!keys.isEmpty()) {
            Throwable throwable = null;
            List<CacheImplementation<?>> errorImplementations = new ArrayList<>();
            Set<String> loaderKeys = tokeyMap(keys, filter).keySet();
            for (CacheImplementation<V> implementation : implementations) {
                try {
                    implementation.deleteAll(loaderKeys);
                } catch (Throwable t) {
                    if (throwable == null) {
                        throwable = t;
                    }
                    errorImplementations.add(implementation);
                }
            }
            if (throwable != null) {
                CacheImpl<K, V> errorCache =
                        new CacheImpl<>(
                                keyPrefix,
                                CacheBinder.of(errorImplementations),
                                loader,
                                locker
                        );
                throw new CacheDeleteException(
                        throwable,
                        errorCache,
                        keys instanceof Set<?> ?
                                (Set<K>)keys :
                                new LinkedHashSet<>(keys)
                );
            }
        }
    }

    private Map<String, K> tokeyMap(Collection<K> keys, CacheFilter filter) {
        String suffix = null;
        if (filter != null) {
            Map<String, Object> args = filter.toCacheArgs();
            if (args != null && !args.isEmpty()) {
                suffix = ":" + args;
            }
        }
        Map<String, K> keyMap = new LinkedHashMap<>();
        for (K key : keys) {
            String loaderKey = keyPrefix + key;
            if (suffix != null) {
                loaderKey += suffix;
            }
            keyMap.put(loaderKey, key);
        }
        return keyMap;
    }

    private class Getter {

        private final Map<String, K> keyMap;

        private final CacheFilter filter;

        private Set<String> missedLoaderKeys;

        private Map<String, V> map =
                new LinkedHashMap<>();

        private List<CacheImplementation<V>> changedImplementations =
                new ArrayList<>();

        private Map<CacheImplementation<V>, Map<String, V>> writeMapMap =
                new IdentityHashMap<>();

        Getter(Collection<K> keys, CacheFilter filter) {
            this.keyMap = tokeyMap(keys, filter);
            this.filter = filter;
            this.missedLoaderKeys = new LinkedHashSet<>(keyMap.keySet());
        }

        Map<K, V> execute() {
            read();
            loadAndWrite();
            Map<K, V> finalMap = new LinkedHashMap<>();
            for (Map.Entry<String, K> keyPair : keyMap.entrySet()) {
                String loaderKey = keyPair.getKey();
                K key = keyPair.getValue();
                V value = map.get(loaderKey);
                if (value != null) {
                    finalMap.put(key, value);
                }
            }
            return finalMap;
        }

        private void read() {
            for (CacheImplementation<V> implementation : implementations) {
                Map<String, V> cachedMap = implementation.getAll(missedLoaderKeys);
                map.putAll(cachedMap);
                missedLoaderKeys.removeAll(cachedMap.keySet());
                if (missedLoaderKeys.isEmpty()) {
                    break;
                }
                Map<String, V> writeMap = new LinkedHashMap<>();
                for (String missedLoaderKey : missedLoaderKeys) {
                    writeMap.put(missedLoaderKey, null);
                }
                changedImplementations.add(implementation);
                writeMapMap.put(implementation, writeMap);
            }
        }

        private void loadAndWrite() {
            if (!missedLoaderKeys.isEmpty()) {
                if (locker == null) {
                    loadAndWriteImpl();
                } else {
                    locker.lockAll(missedLoaderKeys);
                    try {
                        loadAndWriteImpl();
                    } finally {
                        locker.unlockAll(missedLoaderKeys);
                    }
                }
            }
        }

        private void loadAndWriteImpl() {
            if (!missedLoaderKeys.isEmpty()) {
                Map<K, V> loadedMap = loader
                        .loadAll(
                                missedLoaderKeys.stream().map(keyMap::get).collect(Collectors.toSet()),
                                filter
                        );
                for (String missedLoaderKey : missedLoaderKeys) {
                    K missedKey = keyMap.get(missedLoaderKey);
                    map.put(missedLoaderKey, loadedMap.get(missedKey));
                }
                write();
            }
        }

        private void write() {
            for (CacheImplementation<V> implementation : changedImplementations) {
                Map<String, V> writeMap = writeMapMap.get(implementation);
                for (Map.Entry<String, V> nestedEntry : writeMap.entrySet()) {
                    nestedEntry.setValue(map.get(nestedEntry.getKey()));
                }
                if (!implementation.isNullSavable()) {
                    writeMap.values().removeIf(Objects::isNull);
                    if (writeMap.isEmpty()) {
                        continue;
                    }
                }
                try {
                    implementation.setAll(writeMap);
                } catch (RuntimeException | Error ex) {
                    LOGGER.error("Failed to write loaded data to cache implementation" + implementation, ex);
                }
            }
        }
    }
}
