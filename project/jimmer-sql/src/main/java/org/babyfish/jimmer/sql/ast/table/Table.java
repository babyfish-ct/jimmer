package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import javax.persistence.criteria.JoinType;
import java.util.function.Function;

public interface Table<E> extends Selection<E> {

    Predicate eq(Table<E> other);

    Predicate isNull();

    Predicate isNotNull();

    NumericExpression<Long> count();

    NumericExpression<Long> count(boolean distinct);

    <XE extends Expression<?>> XE get(String prop);

    <XT extends Table<?>> XT join(String prop);

    <XT extends Table<?>> XT join(String prop, JoinType joinType);

    <XE, XT extends Table<XE>> XT inverseJoin(
            Class<XE> targetType,
            String backProp
    );

    <XE, XT extends Table<XE>> XT inverseJoin(
            Class<XE> targetType,
            String backProp,
            JoinType joinType
    );

    <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<E>> backPropBlock
    );

    <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<E>> backPropBlock,
            JoinType joinType
    );

    Selection<E> fetch(Fetcher<E> fetcher);
}
