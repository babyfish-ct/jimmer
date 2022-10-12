package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;

import java.util.function.Function;

/**
 * Used by mapped super class
 *
 * 1. It is super class of {@link Table}
 * 2. It must be decorated by {@link PropsFor}
 */
public interface Props {

    <XE extends Expression<?>> XE get(String prop);

    <XT extends Table<?>> XT join(String prop);

    <XT extends Table<?>> XT join(String prop, JoinType joinType);

    <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs);

    <XT extends Table<?>> XT inverseJoin(ImmutableProp prop);

    <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType);

    <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop);

    <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType);

    <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    );

    <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    );
}
