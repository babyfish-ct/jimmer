package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fluent.FluentTable;

import java.util.function.Function;

public abstract class AbstractTableWrapper<E> implements TableWrapper<E>, FluentTable<E> {

    private String joinDisabledReason;

    protected TableImplementor<E> _raw;

    public AbstractTableWrapper(TableImplementor<E> raw, String joinDisabledReason) {
        this._raw = raw;
        this.joinDisabledReason = joinDisabledReason;
    }

    // For fluent-API
    @SuppressWarnings("unchecked")
    @Override
    public void bind(Table<E> raw) {
        if (_raw != null) {
            throw new IllegalStateException("The current table wrapper has been bound");
        }
        if (raw == null) {
            throw new IllegalArgumentException("raw cannot be null");
        }
        _raw = TableWrappers.unwrap(raw);
    }

    @Override
    public ImmutableType getImmutableType() {
        return raw().getImmutableType();
    }

    @Override
    public Predicate eq(Table<E> other) {
        return raw().eq(other);
    }

    @Override
    public Predicate isNull() {
        return raw().isNull();
    }

    @Override
    public Predicate isNotNull() {
        return raw().isNotNull();
    }

    @Override
    public NumericExpression<Long> count() {
        return raw().count();
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        return raw().count(distinct);
    }

    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        return raw().get(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        validateTableJoin();
        return raw().join(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        validateTableJoin();
        return raw().join(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        validateTableJoin();
        return raw().join(prop, joinType, treatedAs);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        validateTableJoin();
        return raw().inverseJoin(prop);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        validateTableJoin();
        return raw().inverseJoin(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        validateTableJoin();
        return raw().inverseJoin(prop);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        validateTableJoin();
        return raw().inverseJoin(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        validateTableJoin();
        return raw().inverseJoin(targetTableType, backPropBlock);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
        validateTableJoin();
        return raw().inverseJoin(targetTableType, backPropBlock, joinType);
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        return raw().fetch(fetcher);
    }

    @Override
    public TableEx<E> asTableEx() {
        return raw().asTableEx();
    }

    @Override
    public TableImplementor<E> unwrap() {
        return raw();
    }

    @Override
    public String getJoinDisabledReason() {
        return joinDisabledReason;
    }

    @Override
    public int hashCode() {
        return raw().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return raw().equals(obj);
    }

    @Override
    public String toString() {
        return raw().toString();
    }

    private TableImplementor<E> raw() {
        TableImplementor<E> raw = _raw;
        if (raw == null) {
            throw new IllegalStateException("FluentTable has not been bound");
        }
        return raw;
    }

    private void validateTableJoin() {
        if (joinDisabledReason != null) {
            throw new IllegalStateException("Table join is disabled because " + joinDisabledReason);
        }
    }
}
