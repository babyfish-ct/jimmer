package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;

import java.util.function.Function;

/**
 * Used by mapped super class
 *
 * 1. It is super class of {@link Table}
 * 2. It must be decorated by {@link PropsFor}
 */
public interface Props {

    ImmutableType getImmutableType();

    <X> PropExpression<X> get(ImmutableProp prop);

    <X> PropExpression<X> get(String prop);

    <X> PropExpression<X> getId();

    <X> PropExpression<X> getAssociatedId(ImmutableProp prop);

    <X> PropExpression<X> getAssociatedId(String prop);

    <XT extends Table<?>> XT join(ImmutableProp prop);

    <XT extends Table<?>> XT join(String prop);

    <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType);

    <XT extends Table<?>> XT join(String prop, JoinType joinType);

    <XT extends Table<?>> XT join(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs);

    <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs);

    <X> PropExpression<X> inverseGetAssociatedId(ImmutableProp prop);

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

    <XT extends Table<?>> Predicate exists(String prop, Function<XT, Predicate> block);

    <XT extends Table<?>> Predicate exists(ImmutableProp prop, Function<XT, Predicate> block);
}
