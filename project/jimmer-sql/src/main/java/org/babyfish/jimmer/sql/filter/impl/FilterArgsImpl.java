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
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FilterArgsImpl<P extends Props> implements FilterArgs<P> {

    private static final String JOIN_DISABLED_REASON =
            "The table for cacheable filter is not allow to join with other tables";

    private static final String SUB_QUERY_DISABLED_MESSAGE =
            "The cacheable filter cannot be used to create sub query";

    private final Sortable sortable;

    private final P props;

    private boolean forCache;

    @SuppressWarnings("unchecked")
    public FilterArgsImpl(Sortable sortable, Props props, boolean forCache) {
        this.sortable = sortable;
        if (forCache) {
            if (props instanceof TableImplementor<?>) {
                props = new UntypedTableProxy<>((TableImplementor<?>)props);
            } else {
                props = ((TableProxy<?>)props).__disableJoin(JOIN_DISABLED_REASON);
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

    @Override
    public MutableSubQuery createSubQuery(TableProxy<?> table) {
        return sortable.createSubQuery(table);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>>
    MutableSubQuery createAssociationSubQuery(AssociationTable<SE, ST, TE, TT> table) {
        return sortable.createAssociationSubQuery(table);
    }

    public AbstractMutableQueryImpl unwrap() {
        return (AbstractMutableQueryImpl) sortable;
    }

    private static class UntypedTableProxy<E> implements TableProxy<E> {

        private final TableImplementor<E> table;

        private final String joinDisabledReason;

        UntypedTableProxy(TableImplementor<E> table) {
            this(table, JOIN_DISABLED_REASON);
        }

        UntypedTableProxy(TableImplementor<E> table, String joinDisabledReason) {
            this.table = table;
            this.joinDisabledReason = joinDisabledReason;
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
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(Class<XT> targetTableType, Function<XT, ? extends Table<?>> backPropBlock) {
            throw new IllegalStateException(joinDisabledReason);
        }

        @Override
        public <XT extends Table<?>> XT inverseJoin(Class<XT> targetTableType, Function<XT, ? extends Table<?>> backPropBlock, JoinType joinType) {
            throw new IllegalStateException(joinDisabledReason);
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
        public Table<?> __parent() {
            return table.getParent();
        }

        @Override
        public ImmutableProp __prop() {
            return table.getJoinProp();
        }

        @Override
        public TableImplementor<E> __unwrap() {
            return table;
        }

        @Override
        public TableImplementor<E> __resolve(RootTableResolver resolver) {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <P extends TableProxy<E>> P __disableJoin(String reason) {
            return (P) new UntypedTableProxy<>(table, reason);
        }
    }
}
