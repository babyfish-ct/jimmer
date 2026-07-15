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
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
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
        Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(type);
        if (concreteTypes.isEmpty()) {
            throw new ExecutionException(
                    "Cannot query inheritance entity type \"" +
                            type +
                            "\" because it is abstract and has no instantiable type"
            );
        }
        return new DiscriminatorPredicate(
                this,
                inheritanceInfo.getDiscriminatorProp(),
                DiscriminatorPredicate.values(inheritanceInfo, type)
        );
    }

    default org.babyfish.jimmer.sql.ast.Predicate instanceOf(ImmutableType targetType) {
        ImmutableType type = getImmutableType();
        if (!type.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            targetType +
                            "\" is not a derived type of \"" +
                            type +
                            "\""
            );
        }
        return discriminatorPredicate(targetType);
    }

    default org.babyfish.jimmer.sql.ast.Predicate exactType(ImmutableType targetType) {
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || !type.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            targetType +
                            "\" is not a derived type of \"" +
                            type +
                            "\""
            );
        }
        if (!targetType.isInstantiable()) {
            throw new ExecutionException(
                    "Cannot check whether table \"" +
                            this +
                            "\" is exact type \"" +
                            targetType +
                            "\" because it is abstract"
            );
        }
        String value = targetType.getDiscriminatorValue();
        if (value == null) {
            throw new ExecutionException(
                    "Cannot check whether table \"" +
                            this +
                            "\" is exact type \"" +
                            targetType +
                            "\" because it has no discriminator value"
            );
        }
        return new DiscriminatorPredicate(
                this,
                inheritanceInfo.getDiscriminatorProp(),
                inheritanceInfo.discriminatorValue(value)
        );
    }

    default org.babyfish.jimmer.sql.ast.Predicate discriminatorPredicate(ImmutableType targetType) {
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || !type.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                    "The type \"" +
                            targetType +
                            "\" is not a derived type of \"" +
                            type +
                            "\""
            );
        }
        Collection<ImmutableType> concreteTypes = inheritanceInfo.getConcreteTypes(targetType);
        if (concreteTypes.isEmpty()) {
            throw new ExecutionException(
                    "Cannot check whether table \"" +
                            this +
                            "\" is instance of \"" +
                            targetType +
                            "\" because it is abstract and has no instantiable type"
            );
        }
        return new DiscriminatorPredicate(
                this,
                inheritanceInfo.getDiscriminatorProp(),
                DiscriminatorPredicate.values(inheritanceInfo, targetType)
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
                            "\" because it is abstract and has no instantiable type"
            );
        }
        if (concreteTypes.size() == 1 && concreteTypes.iterator().next() == type) {
            return null;
        }
        return inheritanceInfo.getDiscriminatorProp();
    }

    default boolean isJoinedTypeBranchRoot() {
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getRootType() != type;
    }

    @Nullable
    default ImmutableType joinedTypeMainTableType() {
        if (isTreated()) {
            return null;
        }
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getRootType() == type) {
            return null;
        }
        return getParent() == null ? inheritanceInfo.getRootType() : type;
    }

    @Nullable
    default ImmutableType joinedTypeTableType(@Nullable ImmutableProp prop) {
        ImmutableType mainType = joinedTypeMainTableType();
        if (mainType == null || prop == null) {
            return null;
        }
        ImmutableType type = getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        ImmutableType stageType = prop.isId() || prop.toOriginal().isId() ?
                mainType :
                inheritanceInfo.getTableTypeForProp(prop, type);
        return stageType;
    }

    @Nullable
    default ImmutableType joinedTypeAdditionalTableType(@Nullable ImmutableProp prop) {
        ImmutableType mainType = joinedTypeMainTableType();
        ImmutableType stageType = joinedTypeTableType(prop);
        return stageType != mainType ? stageType : null;
    }

    default boolean isRootTableProp(ImmutableProp prop) {
        InheritanceInfo inheritanceInfo = getImmutableType().getInheritanceInfo();
        if (inheritanceInfo == null) {
            return true;
        }
        ImmutableType type = getImmutableType();
        if (inheritanceInfo.getRootType() == type) {
            return true;
        }
        return inheritanceInfo.isPropAvailableInTable(prop.toOriginal(), inheritanceInfo.getRootType());
    }

    static String joinedTypeBranchAlias(SqlBuilder builder, TableImplementor<?> table) {
        return builder.alias(table.realTableForRender(builder)) + "_sub";
    }

    static String joinedTypeStageAlias(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType stageType
    ) {
        ImmutableType type = table.getImmutableType();
        ImmutableType mainType = table.joinedTypeMainTableType();
        if (stageType == mainType) {
            return builder.alias(table.realTableForRender(builder));
        }
        if (table.getParent() == null && stageType == type) {
            return joinedTypeBranchAlias(builder, table);
        }
        String alias = builder.alias(table.realTableForRender(builder));
        return alias +
                (alias.endsWith("_") ? "_" : "__") +
                stageType.getJavaClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    static boolean isJoinedTypeBranchTableRendered(
            AbstractSqlBuilder<?> builder,
            TableImplementor<?> table,
            ImmutableType stageType
    ) {
        QueryRenderContext queryRenderContext = builder.getQueryRenderContext();
        if (queryRenderContext != null) {
            return stageType == table.joinedTypeMainTableType() ||
                    queryRenderContext.isJoinedTypeBranchTableRequired(table, stageType);
        }
        AstContext astContext = builder.getAstContext();
        return stageType == table.getImmutableType() &&
                astContext != null &&
                astContext.isJoinedTypeBranchTableRendered(table);
    }

    /**
     * Resolves the alias of an additional physical JOINED-inheritance stage when
     * a join source foreign key does not live in the source table's main stage.
     *
     * @return the physical stage alias, or {@code null} when the main table alias
     * should be used.
     */
    @Nullable
    static String joinedTypeBranchForeignKeyAlias(
            AbstractSqlBuilder<?> builder,
            TableImplementor<?> table
    ) {
        if (table.isInverse()) {
            return null;
        }
        return joinedTypeBranchForeignKeyAlias(builder, table.getParent(), table.getJoinProp());
    }

    /**
     * The variant of {@link #joinedTypeBranchForeignKeyAlias(AbstractSqlBuilder, TableImplementor)}
     * for call sites where the join source and the join property are already known
     * and the inverse case is excluded by the caller.
     */
    @Nullable
    static String joinedTypeBranchForeignKeyAlias(
            AbstractSqlBuilder<?> builder,
            TableImplementor<?> parent,
            ImmutableProp joinProp
    ) {
        AstContext astContext = builder.getAstContext();
        if (astContext == null ||
                parent == null ||
                joinProp == null ||
                astContext.isJoinedTypeBranchUpdateTarget(parent)) {
            return null;
        }
        ImmutableType stageType = parent.joinedTypeAdditionalTableType(joinProp);
        if (stageType == null ||
                !isJoinedTypeBranchTableRendered(builder, parent, stageType)) {
            return null;
        }
        return joinedTypeStageAlias(builder.assertSimple(), parent, stageType);
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
