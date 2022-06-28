package org.babyfish.jimmer.sql.cache.impl;

import org.babyfish.jimmer.sql.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CacheImpl<K, V> implements Cache<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheImpl.class);

    private final String keyPrefix;

    private final CacheProvider<V>[] providers;

    private final CacheLoader<K, V> loader;

    private final CacheLocker locker;

    @SuppressWarnings({"unchecked", "rawtype"})
    public CacheImpl(
            String keyPrefix,
            CacheProvider<V> provider,
            CacheLoader<K, V> loader,
            CacheLocker locker
    ) {
        Objects.requireNonNull(keyPrefix, "keyPrefix cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        this.keyPrefix = keyPrefix;
        if (provider instanceof CompositeProvider<?>) {
            providers = ((CompositeProvider<V>) provider).providers();
        } else {
            providers = (CacheProvider<V>[])new CacheProvider[] { provider };
        }
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
        try (
                OperationContext<K, V> ctx = new OperationContext<>(
                        keyPrefix,
                        keys,
                        filter,
                        loader,
                        locker,
                        providers
                )
        ) {
            Map<String, V> map = providers[0].getAll(
                    ctx.storageKeys(),
                    new CacheChainImpl<>(ctx, 1)
            );
            return ctx.toMap(map);
        }
    }

    @Override
    public void delete(K key, CacheFilter filter) {
        deleteAll(Collections.singleton(key), filter);
    }

    @Override
    public void deleteAll(Collection<K> keys, CacheFilter filter) {
        if (!keys.isEmpty()) {
            KeyManager<K, V> keyManager = new KeyManager<>(
                    keyPrefix,
                    keys,
                    filter
            );
            Throwable throwable = null;
            for (CacheProvider<V> provider : providers) {
                try {
                    provider.deleteAll(keyManager.storageKeys());
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }
}
