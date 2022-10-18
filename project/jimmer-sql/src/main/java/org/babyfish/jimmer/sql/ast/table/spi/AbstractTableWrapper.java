package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fluent.FluentTable;

import java.util.function.Function;

public abstract class AbstractTableWrapper<E> implements Table<E>, FluentTable<E> {

    protected Table<E> _raw;

    public AbstractTableWrapper(Table<E> raw) {
        this._raw = raw;
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
        _raw = (Table<E>) TableWrappers.unwrap(raw);
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
        return raw().join(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        return raw().join(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        return raw().join(prop, joinType, treatedAs);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        return raw().inverseJoin(prop);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        return raw().inverseJoin(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        return raw().inverseJoin(prop);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        return raw().inverseJoin(prop, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        return raw().inverseJoin(targetTableType, backPropBlock);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
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

    public Table<E> __unwrap() {
        return raw();
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

    private Table<E> raw() {
        Table<E> raw = _raw;
        if (raw == null) {
            throw new IllegalStateException("FluentTable has not been bound");
        }
        return raw;
    }
}
