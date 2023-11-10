package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.DraftHandler;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.IdOnlyFetchType;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;
import org.babyfish.jimmer.sql.meta.SqlContext;
import org.babyfish.jimmer.sql.meta.impl.SqlContextCache;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public class IdAndKeyFetchers {

    private static final SqlContextCache<IdAndKeyFetchers> INSTANCE_CACHE =
            new SqlContextCache<>(IdAndKeyFetchers::new);

    private final TypeCache<Fetcher<ImmutableSpi>> fetcherCache =
            new TypeCache<>(this::createFetcher, false);

    private final JSqlClientImplementor sqlClient;

    private IdAndKeyFetchers(SqlContext sqlContext) {
        sqlClient = (JSqlClientImplementor) sqlContext;
    }

    public static Fetcher<ImmutableSpi> getFetcher(JSqlClientImplementor sqlClient, ImmutableType type) {
        return INSTANCE_CACHE.get(sqlClient).getFetcher(type);
    }

    private Fetcher<ImmutableSpi> getFetcher(ImmutableType type) {
        return fetcherCache.get(type);
    }

    @SuppressWarnings("unchecked")
    private Fetcher<ImmutableSpi> createFetcher(ImmutableType type) {
        FetcherImplementor<ImmutableSpi> fetcher =
                new FetcherImpl<>((Class<ImmutableSpi>)type.getJavaClass());
        fetcher = fetcher.add(type.getIdProp().getName());
        for (ImmutableProp keyProp : type.getKeyProps()) {
            fetcher = fetcher.add(keyProp.getName(), IdOnlyFetchType.RAW);
        }
        DraftHandler<?, ?> handler = sqlClient.getDraftHandlers(type);
        if (handler != null) {
            for (ImmutableProp prop : handler.dependencies()) {
                fetcher = fetcher.add(prop.getName(), IdOnlyFetchType.RAW);
            }
        }
        return fetcher;
    }
}
