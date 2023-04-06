package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Collection;
import java.util.List;

public class MicroServiceExporter {

    private final JSqlClient sqlClient;

    public MicroServiceExporter(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @SuppressWarnings("unchecked")
    public List<ImmutableSpi> findByIds(
            Collection<?> ids,
            Fetcher<?> fetcher
    ) {
        if (sqlClient.getFilters().getFilter(fetcher.getImmutableType()) == null) {
            return sqlClient.getEntities().findByIds((Fetcher<ImmutableSpi>) fetcher, ids);
        }
        return sqlClient
                .caches(cfg -> cfg.disable(fetcher.getImmutableType()))
                .getEntities()
                .findByIds((Fetcher<ImmutableSpi>) fetcher, ids);
    }

    @SuppressWarnings("unchecked")
    public List<Tuple2<Object, ImmutableSpi>> findByAssociatedIds(
            ImmutableProp prop,
            Collection<?> targetIds,
            Fetcher<?> fetcher
    ) {
        MutableRootQueryImpl<Table<ImmutableSpi>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        prop.getDeclaringType(),
                        ExecutionPurpose.EXPORT,
                        false
                );
        PropExpression<Object> targetIdExpr = query
                .getTable()
                .join(prop.getName())
                .get(prop.getTargetType().getIdProp().getName());
        query.where(targetIdExpr.in((Collection<Object>) targetIds));
        query.freeze();
        return query
                .select(
                        targetIdExpr,
                        ((Table<ImmutableSpi>)query.getTable()).fetch(
                                (Fetcher<ImmutableSpi>) fetcher
                        )
                )
                .execute();
    }
}
