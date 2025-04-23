package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.table.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
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

    @Override
    public TableImplementor<?> resolveRootTable(Table<?> table) {
        for (TypedRootQuery query : this.queries) {
            TableImplementor<?> tableImplementor;
            if (query instanceof BaseTableQueryImplementor<?, ?>) {
                tableImplementor = ((BaseTableQueryImplementor<?, ?>)query).resolveRootTable(table);
            } else {
                MutableRootQueryImpl<?> baseQuery = ((ConfigurableRootQueryImpl<?, ?>) query).getBaseQuery();
                tableImplementor = AbstractTypedTable.__refEquals(baseQuery.getTable(), table) ?
                        baseQuery.getTableImplementor() :
                        null;
            }
            if (tableImplementor != null) {
                return tableImplementor;
            }
        }
        return null;
    }
}
