package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.impl.util.ClassCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;

import java.sql.Connection;
import java.util.*;

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

    private static final ClassCache<Fetcher<?>> CACHE =
            new ClassCache<>(ObjectCacheFetchers::create);

    @SuppressWarnings("unchecked")
    static <V> Fetcher<V> of(Class<V> type) {
        return (Fetcher<V>) CACHE.get(type);
    }

    private static Fetcher<?> create(Class<?> type) {
        return create(ImmutableType.get(type), Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<?> create(ImmutableType immutableType, Set<String> inheritedPropNames) {
        Fetcher<?> fetcher = new FetcherImpl<>((Class<Object>) immutableType.getJavaClass());
        for (ImmutableProp prop : immutableType.getObjectCacheProps().values()) {
            if (inheritedPropNames.contains(prop.getName())) {
                continue;
            }
            ImmutableProp idViewProp = prop.getIdViewProp();
            ImmutableProp fetchedProp = idViewProp != null ? idViewProp : prop;
            if (prop.isReference(TargetLevel.PERSISTENT)) {
                fetcher = fetcher.add(fetchedProp.getName(), IdOnlyFetchType.RAW);
            } else {
                fetcher = fetcher.add(fetchedProp.getName());
            }
        }
        Set<String> propNames = new HashSet<>(inheritedPropNames);
        propNames.addAll(immutableType.getObjectCacheProps().keySet());
        List<ImmutableType> derivedTypes = new ArrayList<>(immutableType.getDirectDerivedTypes());
        derivedTypes.sort(Comparator.comparing(it -> it.getJavaClass().getName()));
        for (ImmutableType derivedType : derivedTypes) {
            fetcher = ((FetcherImplementor<?>) fetcher).__forType(
                    create(derivedType, propNames)
            );
        }
        return fetcher;
    }
}
