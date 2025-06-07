package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.SimpleBaseTables;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ConfigurableBaseQueryImpl<T extends BaseTable>
extends AbstractConfigurableTypedQueryImpl
implements ConfigurableBaseQuery<T>, TypedBaseQueryImplementor<T> {

    private final T baseTable;

    private MergedBaseQueryImpl<T> mergedBy;

    @SuppressWarnings("unchecked")
    ConfigurableBaseQueryImpl(List<Selection<?>> selections, MutableBaseQueryImpl mutableQuery) {
        super(
                new TypedQueryData(selections),
                mutableQuery
        );
        this.baseTable = (T) SimpleBaseTables.of(this, selections);
    }

    private ConfigurableBaseQueryImpl(T baseTable, TypedQueryData data, AbstractMutableQueryImpl baseQuery) {
        super(data, baseQuery);
        this.baseTable = baseTable;
    }

    @Override
    public MutableBaseQueryImpl getMutableQuery() {
        return (MutableBaseQueryImpl) super.getMutableQuery();
    }

    @Override
    public ConfigurableBaseQuery<T> limit(int limit) {
        return limitImpl(limit, null);
    }

    @Override
    public ConfigurableBaseQuery<T> offset(long offset) {
        return limitImpl(null, offset);
    }

    @Override
    public ConfigurableBaseQuery<T> limit(int limit, long offset) {
        return limitImpl(limit, offset);
    }

    private ConfigurableBaseQuery<T> limitImpl(@Nullable Integer limit, @Nullable Long offset) {
        TypedQueryData data = getData();
        if (limit == null) {
            limit = data.limit;
        }
        if (offset == null) {
            offset = data.offset;
        }
        if (data.limit == limit && data.offset == offset) {
            return this;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' can not be less than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' can not be less than 0");
        }
        return new ConfigurableBaseQueryImpl<>(
                baseTable,
                data.limit(limit, offset),
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableBaseQuery<T> distinct() {
        TypedQueryData data = getData();
        if (data.distinct) {
            return this;
        }
        return new ConfigurableBaseQueryImpl<>(
                baseTable,
                data.distinct(),
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableBaseQuery<T> hint(@Nullable String hint) {
        TypedQueryData data = getData();
        return new ConfigurableBaseQueryImpl<>(
                baseTable,
                data.hint(hint),
                getMutableQuery()
        );
    }

    @Override
    public boolean hasVirtualPredicate() {
        return getMutableQuery().hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        getMutableQuery().resolveVirtualPredicate(ctx);
        return this;
    }

    @Override
    public T asBaseTable() {
        return baseTable;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        getMutableQuery().setParent(visitor.getAstContext().getStatement());
        super.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> abstractBuilder) {
        SqlBuilder builder = abstractBuilder.assertSimple();
        renderTo(builder, builder.getAstContext().getBaseSelectionRender(this));
    }

    @Override
    public TableImplementor<?> resolveRootTable(Table<?> table) {
        MutableBaseQueryImpl mutableQuery = getMutableQuery();
        return AbstractTypedTable.__refEquals(mutableQuery.getTable(), table) ?
                (TableImplementor<?>) mutableQuery.getTableLikeImplementor() :
                null;
    }

    @Override
    public MergedBaseQueryImpl<T> getMergedBy() {
        return mergedBy;
    }

    @Override
    public void setMergedBy(MergedBaseQueryImpl<T> mergedBy) {
        if (this.mergedBy != null && this.mergedBy != mergedBy) {
            throw new IllegalArgumentException(
                    "This current base-query has been merged by another merged base query"
            );
        }
        this.mergedBy = mergedBy;
    }

    private static List<Selection<?>> selections(ConfigurableBaseQueryImpl<?> prev, Selection<?> selection) {
        List<Selection<?>> oldSelections = prev.getSelections();
        List<Selection<?>> selections = new ArrayList<>(oldSelections.size() + 1);
        selections.addAll(oldSelections);
        selections.add(selection);
        return selections;
    }

    static class Simple1Impl<S1 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable1<S1>>
    implements Simple1<S1> {

        Simple1Impl(Selection<?> selection, MutableBaseQueryImpl mutableQuery) {
            super(Collections.singletonList(selection), mutableQuery);
        }

        @Override
        public <T extends Table<?>> Simple2<S1, T> addSelect(T table) {
            return new Simple2Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Simple2<S1, T> addSelect(T expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public <V> Simple2<S1, Expression<V>> addSelect(Expression<V> expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public Simple2<S1, StringExpression> addSelect(StringExpression expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Simple2<S1, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Simple2<S1, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Simple2<S1, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Simple2Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Simple2<S1, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Simple2Impl<>(this, expr);
        }
    }

    private static class Simple2Impl<S1 extends Selection<?>, S2 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable2<S1, S2>>
    implements Simple2<S1, S2> {

        Simple2Impl(Simple1Impl<S1> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Simple3<S1, S2, T> addSelect(T table) {
            return new Simple3Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Simple3<S1, S2, T> addSelect(T expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public <V> Simple3<S1, S2, Expression<V>> addSelect(Expression<V> expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public Simple3<S1, S2, StringExpression> addSelect(StringExpression expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Simple3<S1, S2, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Simple3<S1, S2, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Simple3<S1, S2, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Simple3Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Simple3<S1, S2, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Simple3Impl<>(this, expr);
        }
    }

    private static class Simple3Impl<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable3<S1, S2, S3>>
    implements Simple3<S1, S2, S3> {

        Simple3Impl(Simple2Impl<S1, S2> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }
    }
}
