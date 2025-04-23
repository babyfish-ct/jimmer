package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public class MergedBaseTableQueryImpl<R, B extends BaseTable<R>>
        extends MergedTypedRootQueryImpl<R>
        implements BaseTableQueryImplementor<R, B> {

    @SafeVarargs
    public MergedBaseTableQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedRootQuery<R>... queries
    ) {
        super(sqlClient, operator, queries);
    }

    @Override
    public BaseTableQuery<R, B> union(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(sqlClient, "union", this, other);
    }

    @Override
    public BaseTableQuery<R, B> unionAll(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(sqlClient, "union all", this, other);
    }

    @Override
    public BaseTableQuery<R, B> minus(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(sqlClient, "minus", this, other);
    }

    @Override
    public BaseTableQuery<R, B> intersect(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(sqlClient, "intersect", this, other);
    }

    @Override
    public B asBaseTable() {
        return BaseTables.create(this);
    }
}
