package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.EntitiesImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImplementor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MicroServiceExporter {

    private final JSqlClientImplementor sqlClient;

    public MicroServiceExporter(JSqlClient sqlClient) {
        this.sqlClient = (JSqlClientImplementor) Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
    }

    @SuppressWarnings("unchecked")
    public List<ImmutableSpi> findByIds(
            Collection<?> ids,
            Fetcher<?> fetcher
    ) {
        ImmutableType fetchedType = fetcher.getImmutableType();
        JSqlClient client =
                sqlClient.getFilters().getFilter(fetchedType) == null ?
                        sqlClient :
                        sqlClient.caches(cfg -> cfg.disable(fetchedType));
        Entities entities = ((EntitiesImpl) client.getEntities()).forExporter();
        return entities.findByIds((Fetcher<ImmutableSpi>) fetcher, ids);
    }

    @SuppressWarnings("unchecked")
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) {
        ImmutableType fetchedType = fetcher.getImmutableType();
        if (prop.getDeclaringType() != fetchedType) {
            throw new IllegalArgumentException(
                    "The root entity type of fetcher is \"" +
                            fetchedType +
                            "\" is not declaring type of \"" +
                            prop +
                            "\""
            );
        }
        MutableRootQueryImpl<Table<ImmutableSpi>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        prop.getDeclaringType(),
                        ExecutionPurpose.EXPORT,
                        FilterLevel.DEFAULT
                );
        Expression<Object> targetIdExpr = query
                .<Table<?>>getTable()
                .join(prop.getName())
                .get(prop.getTargetType().getIdProp().getName());
        return query
                .where(targetIdExpr.in((Collection<Object>) targetIds))
                .select(
                        targetIdExpr,
                        ((Table<ImmutableSpi>)query.getTable()).fetch(
                                (Fetcher<ImmutableSpi>) fetcher
                        )
                )
                .execute();
    }
}
