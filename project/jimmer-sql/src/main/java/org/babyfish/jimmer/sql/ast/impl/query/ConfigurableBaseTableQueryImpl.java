package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.table.MapperSelectionImpl;
import org.babyfish.jimmer.sql.ast.mapper.BaseTableMapper;
import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;

import java.util.Collections;

public class ConfigurableBaseTableQueryImpl<T extends TableLike<?>, R, B extends BaseTable<R>>
        extends ConfigurableRootQueryImpl<T, R>
        implements ConfigurableBaseTableQuery<T, R, B>, BaseTableQueryImplementor<R, B> {

    ConfigurableBaseTableQueryImpl(BaseTableMapper<R, B> mapper, MutableRootQueryImpl<T> baseQuery) {
        super(
                new TypedQueryData(
                        Collections.singletonList(
                                new MapperSelectionImpl<>(mapper)
                        )
                ),
                baseQuery
        );
    }

    @Override
    public BaseTableQuery<R, B> union(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(getSqlClient(), "union", this, other);
    }

    @Override
    public BaseTableQuery<R, B> unionAll(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(getSqlClient(), "union all", this, other);
    }

    @Override
    public BaseTableQuery<R, B> minus(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(getSqlClient(), "minus", this, other);
    }

    @Override
    public BaseTableQuery<R, B> intersect(TypedRootQuery<R> other) {
        return new MergedBaseTableQueryImpl<>(getSqlClient(), "intersect", this, other);
    }

    @Override
    public B asBaseTable() {
        return BaseTables.create(this);
    }
}
