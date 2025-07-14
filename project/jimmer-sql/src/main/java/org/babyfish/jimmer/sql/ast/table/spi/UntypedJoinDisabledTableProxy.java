package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.WeakJoinHandle;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class UntypedJoinDisabledTableProxy<E> implements TableProxy<E> {

    private final TableImplementor<E> table;

    private final String joinDisabledReason;

    public UntypedJoinDisabledTableProxy(TableImplementor<E> table, String joinDisabledReason) {
        this.table = table;
        this.joinDisabledReason = "Table join is disabled. " + joinDisabledReason;
    }

    @Override
    public ImmutableType getImmutableType() {
        return table.getImmutableType();
    }

    @Override
    public <X> PropExpression<X> get(String prop) {
        return table.get(prop);
    }

    @Override
    public <X> PropExpression<X> get(ImmutableProp prop) {
        return table.get(prop);
    }

    @Override
    public <X> PropExpression<X> getId() {
        return table.getId();
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(ImmutableProp prop) {
        if (!prop.isColumnDefinition()) {
            throw new IllegalStateException(joinDisabledReason);
        }
        return table.get(prop);
    }

    @Override
    public <X> PropExpression<X> getAssociatedId(String prop) {
        return table.get(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs) {
        throw new IllegalStateException(joinDisabledReason);
    }

    @Override
    public <X> PropExpression<X> inverseGetAssociatedId(ImmutableProp prop) {
        ImmutableProp opposite = prop.getOpposite();
        if (opposite == null || !opposite.isColumnDefinition()) {
            throw new IllegalStateException(joinDisabledReason);
        }
        return table.inverseGetAssociatedId(prop);
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
    public <XT extends Table<?>> Predicate exists(String prop, Function<XT, Predicate> block) {
        return table.exists(prop, block);
    }

    @Override
    public <XT extends Table<?>> Predicate exists(ImmutableProp prop, Function<XT, Predicate> block) {
        return table.exists(prop, block);
    }

    @Override
    public Predicate eq(Table<E> other) {
        return table.eq(other);
    }

    @Override
    public Predicate eq(Example<E> example) {
        return table.eq(example);
    }

    @Override
    public Predicate eq(E example) {
        return table.eq(example);
    }

    @Override
    public Predicate eq(View<E> view) {
        return table.eq(view);
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
    public <V extends View<E>> Selection<V> fetch(Class<V> viewType) {
        return table.fetch(viewType);
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
    public WeakJoinHandle __weakJoinHandle() {
        return table.getWeakJoinHandle();
    }

    @Override
    public boolean __isInverse() {
        return table.isInverse();
    }

    @Override
    public TableImplementor<E> __unwrap() {
        return table;
    }

    @Override
    public TableImplementor<E> __resolve(RootTableResolver resolver) {
        return table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P extends TableProxy<E>> P __disableJoin(String reason) {
        return (P) new UntypedJoinDisabledTableProxy<>(table, reason);
    }

    @Override
    public TableProxy<E> __baseTableOwner(BaseTableOwner baseTableOwner) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable BaseTableOwner __baseTableOwner() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JoinType __joinType() {
        return table.getJoinType();
    }

    @Override
    public int hashCode() {
        return table.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table<?>)) return false;
        return table.equals(o);
    }
}
