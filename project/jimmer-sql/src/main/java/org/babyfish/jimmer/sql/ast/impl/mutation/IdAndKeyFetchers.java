package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.impl.util.StaticCache;

public class IdAndKeyFetchers {

    private static final StaticCache<ImmutableType, Fetcher<ImmutableSpi>> CACHE =
            new StaticCache<>(IdAndKeyFetchers::createFetcher, false);

    private IdAndKeyFetchers() {}

    public static Fetcher<ImmutableSpi> getFetcher(ImmutableType type) {
        return CACHE.get(type);
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<ImmutableSpi> createFetcher(ImmutableType type) {
        Fetcher<ImmutableSpi> fetcher =
                new FetcherImpl<>((Class<ImmutableSpi>)type.getJavaClass());
        fetcher = fetcher.add(type.getIdProp().getName());
        for (ImmutableProp keyProp : type.getKeyProps()) {
            fetcher = fetcher.add(keyProp.getName());
        }
        return fetcher;
    }
}
