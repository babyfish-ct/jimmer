package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface TableImplementor<E> extends TableEx<E>, Ast, TableSelection, TableLikeImplementor<E> {

    @Override
    TableImplementor<?> getParent();

    ImmutableProp getJoinProp();

    boolean isInverse();

    boolean isEmpty(Predicate<TableLikeImplementor<?>> filter);

    boolean isRemote();

    TableRowCountDestructive getDestructive();

    void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode);

    <X> PropExpression<X> get(ImmutableProp prop, boolean rawId);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop);

    <X> TableImplementor<X> joinImplementor(String prop);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType);

    <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType, long order);

    <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType, long order);

    <X> TableImplementor<X> treatAsImplementor(ImmutableType treatedAs, JoinType joinType);

    boolean isTreated();

    <X> TableImplementor<X> inverseJoinImplementor(ImmutableProp prop);

    <X> TableImplementor<X> inverseJoinImplementor(ImmutableProp prop, JoinType joinType, long order);

    <X> TableImplementor<X> inverseJoinImplementor(TypedProp.Association<?, ?> prop);

    <X> TableImplementor<X> inverseJoinImplementor(TypedProp.Association<?, ?> prop, JoinType joinType);

    <X> TableImplementor<X> weakJoinImplementor(
            Class<? extends WeakJoin<?, ?>> weakJoinType,
            JoinType joinType
    );

    <X> TableImplementor<X> weakJoinImplementor(
            Class<? extends Table<?>> targetTableType,
            JoinType joinType,
            WeakJoin<?, ?> weakJoinLambda
    );

    <X> TableImplementor<X> weakJoinImplementor(WeakJoinHandle handle, JoinType joinType, long order);

    <X extends BaseTable> X weakJoinImplementor(X targetBaseTable, WeakJoinHandle handle, JoinType joinType);

    TableImplementor<?> joinFetchImplementor(ImmutableProp prop, BaseTableOwner baseTableOwner);

    @Nullable
    BaseTableOwner getBaseTableOwner();

    TableImplementor<E> baseTableOwner(
            @Nullable BaseTableOwner baseTableOwner
    );

    @Nullable
    default org.babyfish.jimmer.sql.ast.Predicate getDiscriminatorPredicate() {
        if (getParent() != null) {
            return null;
        }
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getRootType() == type) {
            return null;
        }
        List<Object> values = new ArrayList<>();
        Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(type);
        if (concreteTypes.isEmpty()) {
            throw new ExecutionException(
                    "Cannot query inheritance entity type \"" +
                            type +
                            "\" because it is abstract and has no instantiable subtype"
            );
        }
        for (ImmutableType concreteType : concreteTypes) {
            String value = concreteType.getDiscriminatorValue();
            if (value != null) {
                values.add(inheritanceInfo.discriminatorValue(value));
            }
        }
        return new DiscriminatorPredicate(
                this,
                inheritanceInfo.getDiscriminatorProp(),
                values
        );
    }

    default org.babyfish.jimmer.sql.ast.Predicate instanceOf(ImmutableType targetType) {
        ImmutableType type = getImmutableType();
        if (!type.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            targetType +
                            "\" is not subtype of \"" +
                            type +
                            "\""
            );
        }
        return discriminatorPredicate(targetType);
    }

    default org.babyfish.jimmer.sql.ast.Predicate discriminatorPredicate(ImmutableType targetType) {
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || !type.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            targetType +
                            "\" is not subtype of \"" +
                            type +
                            "\""
            );
        }
        List<Object> values = new ArrayList<>();
        Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(targetType);
        if (concreteTypes.isEmpty()) {
            throw new ExecutionException(
                    "Cannot check whether table \"" +
                            this +
                            "\" is instance of \"" +
                            targetType +
                            "\" because it is abstract and has no instantiable subtype"
            );
        }
        for (ImmutableType concreteType : concreteTypes) {
            String value = concreteType.getDiscriminatorValue();
            if (value != null) {
                values.add(inheritanceInfo.discriminatorValue(value));
            }
        }
        return new DiscriminatorPredicate(
                this,
                inheritanceInfo.getDiscriminatorProp(),
                values
        );
    }

    @Nullable
    default ImmutableProp getPolymorphicDiscriminatorProp() {
        if (getParent() != null) {
            return null;
        }
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return null;
        }
        Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(type);
        if (concreteTypes.isEmpty()) {
            throw new ExecutionException(
                    "Cannot query inheritance entity type \"" +
                            type +
                            "\" because it is abstract and has no instantiable subtype"
            );
        }
        if (concreteTypes.size() == 1 && concreteTypes.iterator().next() == type) {
            return null;
        }
        return inheritanceInfo.getDiscriminatorProp();
    }

    default boolean isJoinedSubtypeRoot() {
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type;
    }

    default boolean isJoinedSubtypeTableRequiredBy(@Nullable ImmutableProp prop) {
        if (!isJoinedSubtypeRoot()) {
            return false;
        }
        return prop != null && !isRootTableProp(prop);
    }

    default boolean isRootTableProp(ImmutableProp prop) {
        InheritanceInfo inheritanceInfo = getImmutableType().getInheritanceInfo();
        if (inheritanceInfo == null) {
            return true;
        }
        return prop.toOriginal().getDeclaringType().isAssignableFrom(inheritanceInfo.getRootType());
    }

    void setHasBaseTable();

    static TableImplementor<?> create(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType
    ) {
        if (immutableType instanceof AssociationType) {
            return new AssociationTableImpl<>(
                    statement,
                    (AssociationType) immutableType
            );
        }
        return new TableImpl<>(
                statement,
                immutableType,
                null,
                -1,
                false,
                null,
                null,
                JoinType.INNER,
                false
        );
    }

    enum RenderMode {
        NORMAL,
        FROM_ONLY,
        WHERE_ONLY,
        DEEPER_JOIN_ONLY;
    }
}
