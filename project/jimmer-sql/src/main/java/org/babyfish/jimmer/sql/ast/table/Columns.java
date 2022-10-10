package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;

import java.util.function.Function;

/**
 * Used by mapped super class
 *
 * 1. It is super class of {@link Table}
 * 2. It must be decorated by {@link ColumnsFor}
 */
public interface Columns<E> {

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
}
