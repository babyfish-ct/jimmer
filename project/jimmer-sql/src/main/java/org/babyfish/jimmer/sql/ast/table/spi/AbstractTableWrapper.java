package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

import javax.persistence.criteria.JoinType;

public abstract class AbstractTableWrapper<E> implements Table<E> {

    protected Table<E> raw;

    public AbstractTableWrapper(Table<E> raw) {
        this.raw = raw;
    }

    @Override
    public Predicate eq(Table<E> other) {
        return raw.eq(other);
    }

    @Override
    public NumericExpression<Long> count() {
        return raw.count();
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        return raw.count(distinct);
    }

    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        return raw.get(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        return raw.join(prop);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        return raw.join(prop, joinType);
    }

    @Override
    public <XE, XT extends Table<XE>> XT inverseJoin(Class<XE> targetType, String backProp) {
        return raw.inverseJoin(targetType, backProp);
    }

    @Override
    public <XE, XT extends Table<XE>> XT inverseJoin(Class<XE> targetType, String backProp, JoinType joinType) {
        return raw.inverseJoin(targetType, backProp, joinType);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoinByTable(
            Class<XT> targetTableType,
            String backProp
    ) {
        return raw.inverseJoinByTable(targetTableType, backProp);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoinByTable(
            Class<XT> targetTableType,
            String backProp,
            JoinType joinType
    ) {
        return raw.inverseJoinByTable(targetTableType, backProp, joinType);
    }

    public Table<E> __unwrap() {
        return raw;
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return raw.equals(obj);
    }

    @Override
    public String toString() {
        return raw.toString();
    }
}
