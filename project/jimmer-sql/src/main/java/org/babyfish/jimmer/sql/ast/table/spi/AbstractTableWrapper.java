package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import javax.persistence.criteria.JoinType;
import java.util.function.Function;

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
    public Predicate isNull() {
        return raw.isNull();
    }

    @Override
    public Predicate isNotNull() {
        return raw.isNotNull();
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
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<E>> backPropBlock
    ) {
        return raw.inverseJoin(targetTableType, backPropBlock);
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<E>> backPropBlock,
            JoinType joinType
    ) {
        return raw.inverseJoin(targetTableType, backPropBlock, joinType);
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        return raw.fetch(fetcher);
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
