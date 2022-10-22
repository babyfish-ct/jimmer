package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableWrapper;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FilterArgsImpl<P extends Props> implements FilterArgs<P> {

    private static final String CACHEABLE_TABLE_JOIN_ERROR_MESSAGE =
            "The table for cacheable filter is not allow to join with other tables";

    private static final String CACHEABLE_FILTER_SUB_QUERY_ERROR_MESSAGE =
            "The cacheable filter cannot be used to create sub query";

    private final Sortable sortable;

    private final P props;

    private boolean forCache;

    @SuppressWarnings("unchecked")
    public FilterArgsImpl(Sortable sortable, Props props, boolean forCache) {
        this.sortable = sortable;
        if (forCache) {
            props = TableWrappers.wrap(
                    (Table<?>) props,
                    CACHEABLE_TABLE_JOIN_ERROR_MESSAGE
            );
            if (!(props instanceof TableWrapper<?>)) {
                props = new CacheableTable<>((Table<?>)props);
            }
        }
        this.props = (P)props;
        this.forCache = forCache;
    }

    @Override
    public @NotNull P getTable() {
        return props;
    }

    @Override
    public <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    ) {
        if (forCache) {
            throw new IllegalStateException(CACHEABLE_FILTER_SUB_QUERY_ERROR_MESSAGE);
        }
        return sortable.createSubQuery(tableType, block);
    }

    @Override
    public <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    ) {
        if (forCache) {
            throw new IllegalStateException(CACHEABLE_FILTER_SUB_QUERY_ERROR_MESSAGE);
        }
        return sortable.createWildSubQuery(tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> ConfigurableSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
    ) {
        if (forCache) {
            throw new IllegalStateException(CACHEABLE_FILTER_SUB_QUERY_ERROR_MESSAGE);
        }
        return sortable.createAssociationSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>> block
    ) {
        if (forCache) {
            throw new IllegalStateException(CACHEABLE_FILTER_SUB_QUERY_ERROR_MESSAGE);
        }
        return sortable.createAssociationWildSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return sortable.where(predicates);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?>... expressions) {
        return sortable.orderBy(expressions);
    }

    @Override
    @OldChain
    public Sortable orderBy(Order... orders) {
        return sortable.orderBy(orders);
    }

    public AbstractMutableQueryImpl unwrap() {
        return (AbstractMutableQueryImpl) sortable;
    }

    private static class CacheableTable<E> implements TableWrapper<E> {

        private final TableImplementor<E> table;

        private CacheableTable(Table<E> table) {
            this.table = TableWrappers.unwrap(table);
        }

        @Override
        public ImmutableType getImmutableType() {
            return table.getImmutableType();
        }

        @Override
        public <XE extends Expression<?>> XE get(String prop) {
            return table.get(prop);
        }

        @Override
        public <XT extends Table<?>> XT join(String prop) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(Class<XT> targetTableType, Function<XT, ? extends Table<?>> backPropBlock) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(Class<XT> targetTableType, Function<XT, ? extends Table<?>> backPropBlock, JoinType joinType) {
            throw new IllegalStateException(CACHEABLE_TABLE_JOIN_ERROR_MESSAGE);
        }

        @Override
        public Predicate eq(Table<E> other) {
            return table.eq(other);
        }

        @Override
        public Predicate isNull() {
            return table.isNull();
        }

        @Override
        public Predicate isNotNull() {
            return table.isNotNull();
        }

        @Override
        public NumericExpression<Long> count() {
            return table.count();
        }

        @Override
        public NumericExpression<Long> count(boolean distinct) {
            return table.count(distinct);
        }

        @Override
        public Selection<E> fetch(Fetcher<E> fetcher) {
            return table.fetch(fetcher);
        }

        @Override
        public TableEx<E> asTableEx() {
            return table.asTableEx();
        }

        @Override
        public TableImplementor<E> unwrap() {
            return table;
        }

        @Override
        public String getJoinDisabledReason() {
            return CACHEABLE_TABLE_JOIN_ERROR_MESSAGE;
        }
    }
}
