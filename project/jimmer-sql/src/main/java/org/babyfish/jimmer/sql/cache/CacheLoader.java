package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface CacheLoader<K, V> {

    Map<K, V> loadAll(Collection<K> keys);

    static <K, V> CacheLoader<K, V> objectLoader(
            JSqlClient sqlClient,
            Connection con,
            Class<V> entityType
    ) {
        return keys -> sqlClient
                .getEntities()
                .forConnection(con)
                .findMapByIds(ObjectCacheFetchers.of(entityType), keys);
    }
}

class ObjectCacheFetchers {

    private static final ClassCache<Fetcher<?>> CACHE = new ClassCache<>(
            ObjectCacheFetchers::create,
            false
    );

    @SuppressWarnings("unchecked")
    static <V> Fetcher<V> of(Class<V> type) {
        return (Fetcher<V>) CACHE.get(type);
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<?> create(Class<?> type) {
        ImmutableType immutableType = ImmutableType.get(type);
        Fetcher<?> fetcher = new FetcherImpl<>((Class<Object>) type);
        for (ImmutableProp prop : immutableType.getObjectCacheProps().values()) {
            ImmutableProp idViewProp = prop.getIdViewProp();
            ImmutableProp fetchedProp = idViewProp != null ? idViewProp : prop;
            if (fetchedProp.isReference(TargetLevel.PERSISTENT)) {
                fetcher = fetcher.add(fetchedProp.getName(), IdOnlyFetchType.RAW);
            } else {
                fetcher = fetcher.add(fetchedProp.getName());
            }
        }
        return fetcher;
    }
}
