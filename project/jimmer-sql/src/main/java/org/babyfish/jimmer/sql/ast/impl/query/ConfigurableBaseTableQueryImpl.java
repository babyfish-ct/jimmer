package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.MapperSelectionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mapper.BaseTableMapper;
import org.babyfish.jimmer.sql.ast.query.BaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseTableQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public TableImplementor<?> resolveRootTable(Table<?> table) {
        MutableRootQueryImpl<?> baseQuery = this.getBaseQuery();
        if (!AbstractTypedTable.__refEquals(baseQuery.getTable(), table)) {
            throw new AssertionError("Internal bug, unexpected base table");
        }
        return (TableImplementor<?>) baseQuery.getTableLikeImplementor();
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> abstractBuilder) {
        SqlBuilder builder = abstractBuilder.assertSimple();
        renderTo(builder, builder.getAstContext().getBaseSelectionRender());
    }
}
