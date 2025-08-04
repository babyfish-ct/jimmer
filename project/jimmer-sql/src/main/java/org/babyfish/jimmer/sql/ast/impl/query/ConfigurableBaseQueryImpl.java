package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.base.AbstractBaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.base.*;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.*;

public class ConfigurableBaseQueryImpl<T extends BaseTable>
extends AbstractConfigurableTypedQueryImpl
implements ConfigurableBaseQuery<T>, TypedBaseQueryImplementor<T> {

    private T baseTable;

    private MergedBaseQueryImpl<T> mergedBy;

    ConfigurableBaseQueryImpl(List<Selection<?>> selections, MutableBaseQueryImpl mutableQuery) {
        super(
                new TypedQueryData(selections),
                mutableQuery
        );
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
        return asBaseTable(null, false);
    }

    @Override
    public T asCteBaseTable() {
        return asBaseTable(null, true);
    }

    public T getBaseTable() {
        if (baseTable == null) {
            throw new IllegalArgumentException(
                    "`asBaseTable`/`asCteBaseTable` has not been invoked"
            );
        }
        return baseTable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T asBaseTable(byte[] kotlinSelectionTypes, boolean cte) {
        T baseTable = this.baseTable;
        if (baseTable != null) {
            return AbstractBaseTableSymbol.validateCte(baseTable, cte);
        }
        this.baseTable = baseTable =
            mergedBy != null ?
                    mergedBy.asBaseTable(kotlinSelectionTypes, cte) :
                    (T) BaseTableSymbols.of(this, getData().selections, kotlinSelectionTypes, cte);
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
        if (this.baseTable != null) {
            throw new IllegalStateException(
                    "The base query cannot be merged after its `asBaseTable()` is called"
            );
        }
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

    static class Query1Impl<S1 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable1<S1>>
    implements Query1<S1> {

        Query1Impl(Selection<?> selection, MutableBaseQueryImpl mutableQuery) {
            super(Collections.singletonList(selection), mutableQuery);
        }

        @Override
        public <T extends Table<?>> Query2<S1, T> addSelect(T table) {
            return new Query2Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query2<S1, T> addSelect(T expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public <V> Query2<S1, Expression<V>> addSelect(Expression<V> expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public Query2<S1, StringExpression> addSelect(StringExpression expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query2<S1, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query2<S1, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query2<S1, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query2Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query2<S1, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query2Impl<>(this, expr);
        }
    }

    private static class Query2Impl<S1 extends Selection<?>, S2 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable2<S1, S2>>
    implements Query2<S1, S2> {

        Query2Impl(Query1Impl<S1> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query3<S1, S2, T> addSelect(T table) {
            return new Query3Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query3<S1, S2, T> addSelect(T expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public <V> Query3<S1, S2, Expression<V>> addSelect(Expression<V> expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public Query3<S1, S2, StringExpression> addSelect(StringExpression expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query3<S1, S2, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query3<S1, S2, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query3<S1, S2, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query3Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query3<S1, S2, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query3Impl<>(this, expr);
        }
    }

    private static class Query3Impl<S1 extends Selection<?>, S2 extends Selection<?>, S3 extends Selection<?>>
    extends ConfigurableBaseQueryImpl<BaseTable3<S1, S2, S3>>
    implements Query3<S1, S2, S3> {

        Query3Impl(Query2Impl<S1, S2> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query4<S1, S2, S3, T> addSelect(T table) {
            return new Query4Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query4<S1, S2, S3, T> addSelect(T expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public <V> Query4<S1, S2, S3, Expression<V>> addSelect(Expression<V> expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public Query4<S1, S2, S3, StringExpression> addSelect(StringExpression expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query4<S1, S2, S3, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query4<S1, S2, S3, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query4<S1, S2, S3, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query4Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query4<S1, S2, S3, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query4Impl<>(this, expr);
        }
    }

    private static class Query4Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable4<S1, S2, S3, S4>> implements Query4<S1, S2, S3, S4> {

        Query4Impl(Query3Impl<S1, S2, S3> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query5<S1, S2, S3, S4, T> addSelect(T table) {
            return new Query5Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query5<S1, S2, S3, S4, T> addSelect(T expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public <V> Query5<S1, S2, S3, S4, Expression<V>> addSelect(Expression<V> expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public Query5<S1, S2, S3, S4, StringExpression> addSelect(StringExpression expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query5<S1, S2, S3, S4, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query5<S1, S2, S3, S4, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query5<S1, S2, S3, S4, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query5Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query5<S1, S2, S3, S4, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query5Impl<>(this, expr);
        }
    }

    private static class Query5Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable5<S1, S2, S3, S4, S5>>
    implements Query5<S1, S2, S3, S4, S5> {

        Query5Impl(Query4Impl<S1, S2, S3, S4> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query6<S1, S2, S3, S4, S5, T> addSelect(T table) {
            return new Query6Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query6<S1, S2, S3, S4, S5, T> addSelect(T expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public <V> Query6<S1, S2, S3, S4, S5, Expression<V>> addSelect(Expression<V> expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public Query6<S1, S2, S3, S4, S5, StringExpression> addSelect(StringExpression expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query6<S1, S2, S3, S4, S5, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query6<S1, S2, S3, S4, S5, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query6<S1, S2, S3, S4, S5, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query6Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query6<S1, S2, S3, S4, S5, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query6Impl<>(this, expr);
        }
    }

    private static class Query6Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable6<S1, S2, S3, S4, S5, S6>>
    implements Query6<S1, S2, S3, S4, S5, S6> {

        Query6Impl(Query5Impl<S1, S2, S3, S4, S5> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query7<S1, S2, S3, S4, S5, S6, T> addSelect(T table) {
            return new Query7Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query7<S1, S2, S3, S4, S5, S6, T> addSelect(T expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public <V> Query7<S1, S2, S3, S4, S5, S6, Expression<V>> addSelect(Expression<V> expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public Query7<S1, S2, S3, S4, S5, S6, StringExpression> addSelect(StringExpression expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query7<S1, S2, S3, S4, S5, S6, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query7<S1, S2, S3, S4, S5, S6, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query7<S1, S2, S3, S4, S5, S6, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query7Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query7<S1, S2, S3, S4, S5, S6, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query7Impl<>(this, expr);
        }
    }

    private static class Query7Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable7<S1, S2, S3, S4, S5, S6, S7>>
    implements Query7<S1, S2, S3, S4, S5, S6, S7> {

        Query7Impl(Query6Impl<S1, S2, S3, S4, S5, S6> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T table) {
            return new Query8Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query8<S1, S2, S3, S4, S5, S6, S7, T> addSelect(T expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public <V> Query8<S1, S2, S3, S4, S5, S6, S7, Expression<V>> addSelect(Expression<V> expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public Query8<S1, S2, S3, S4, S5, S6, S7, StringExpression> addSelect(StringExpression expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query8<S1, S2, S3, S4, S5, S6, S7, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query8<S1, S2, S3, S4, S5, S6, S7, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query8<S1, S2, S3, S4, S5, S6, S7, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query8Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query8<S1, S2, S3, S4, S5, S6, S7, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query8Impl<>(this, expr);
        }
    }

    private static class Query8Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable8<S1, S2, S3, S4, S5, S6, S7, S8>>
            implements Query8<S1, S2, S3, S4, S5, S6, S7, S8> {

        Query8Impl(Query7Impl<S1, S2, S3, S4, S5, S6, S7> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }

        @Override
        public <T extends Table<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T table) {
            return new Query9Impl<>(this, table);
        }

        @Override
        public <T extends AbstractTypedEmbeddedPropExpression<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, T> addSelect(T expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public <V> Query9<S1, S2, S3, S4, S5, S6, S7, S8, Expression<V>> addSelect(Expression<V> expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public Query9<S1, S2, S3, S4, S5, S6, S7, S8, StringExpression> addSelect(StringExpression expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public <V extends Number & Comparable<V>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, NumericExpression<V>> addSelect(NumericExpression<V> expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public <V extends Comparable<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, ComparableExpression<V>> addSelect(ComparableExpression<V> expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public <V extends Date> Query9<S1, S2, S3, S4, S5, S6, S7, S8, DateExpression<V>> addSelect(DateExpression<V> expr) {
            return new Query9Impl<>(this, expr);
        }

        @Override
        public <V extends Temporal & Comparable<?>> Query9<S1, S2, S3, S4, S5, S6, S7, S8, TemporalExpression<V>> addSelect(TemporalExpression<V> expr) {
            return new Query9Impl<>(this, expr);
        }
    }

    private static class Query9Impl<
            S1 extends Selection<?>,
            S2 extends Selection<?>,
            S3 extends Selection<?>,
            S4 extends Selection<?>,
            S5 extends Selection<?>,
            S6 extends Selection<?>,
            S7 extends Selection<?>,
            S8 extends Selection<?>,
            S9 extends Selection<?>
    > extends ConfigurableBaseQueryImpl<BaseTable9<S1, S2, S3, S4, S5, S6, S7, S8, S9>>
    implements Query9<S1, S2, S3, S4, S5, S6, S7, S8, S9> {

        Query9Impl(Query8Impl<S1, S2, S3, S4, S5, S6, S7, S8> prev, Selection<?> selection) {
            super(selections(prev, selection), prev.getMutableQuery());
        }
    }
}
