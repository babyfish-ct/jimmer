package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.RootTableResolver;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.function.Function;

public class UntypedJoinDisabledTableProxy<E> implements TableProxy<E> {

    private final TableImplementor<E> table;

    private final String joinDisabledReason;

    public UntypedJoinDisabledTableProxy(TableImplementor<E> table, String joinDisabledReason) {
        this.table = table;
        this.joinDisabledReason = "Table join is disabled because " + joinDisabledReason;
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
        return (P) new UntypedJoinDisabledTableProxy<>(table, reason);
    }
}
