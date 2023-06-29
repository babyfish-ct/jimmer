package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;

public class IdAndKeyFetchers {

    private static final TypeCache<Fetcher<ImmutableSpi>> CACHE =
            new TypeCache<>(IdAndKeyFetchers::createFetcher, false);

    private IdAndKeyFetchers() {}

    public static Fetcher<ImmutableSpi> getFetcher(ImmutableType type) {
        return CACHE.get(type);
    }

    @SuppressWarnings("unchecked")
    private static Fetcher<ImmutableSpi> createFetcher(ImmutableType type) {
        FetcherImplementor<ImmutableSpi> fetcher =
                new FetcherImpl<>((Class<ImmutableSpi>)type.getJavaClass());
        fetcher = fetcher.add(type.getIdProp().getName());
        for (ImmutableProp keyProp : type.getKeyProps()) {
            fetcher = fetcher.add(keyProp.getName());
        }
        return fetcher;
    }
}
