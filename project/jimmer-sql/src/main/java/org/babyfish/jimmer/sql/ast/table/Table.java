package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;

import javax.persistence.criteria.JoinType;

public interface Table<E> extends Selection<E> {

    <XE extends Expression<?>> XE get(String prop);

    <XT extends Table<?>> XT join(String prop);

    <XT extends Table<?>> XT join(String prop, JoinType joinType);

    <XT extends Table<?>> XT inverseJoin(
            Class<?> targetType,
            String backProp
    );

    <XT extends Table<?>> XT inverseJoin(
            Class<?> targetType,
            String backProp,
            JoinType joinType
    );
}
