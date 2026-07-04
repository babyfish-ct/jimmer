package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.sql.runtime.TableUsedState;

import java.util.Locale;

final class MutationJoinRenderSupport {

    private MutationJoinRenderSupport() {}

    static boolean hasUsedChild(TableImplementor<?> table, AstContext astContext) {
        for (RealTable childTable : table.realTable(astContext)) {
            if (astContext.getTableUsedState(childTable) == TableUsedState.USED) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFirstLevelJoinUnsupportedByFromOnly(TableImplementor<?> table, AstContext astContext) {
        for (RealTable childTable : table.realTable(astContext)) {
            if (astContext.getTableUsedState(childTable) != TableUsedState.USED) {
                continue;
            }
            TableLikeImplementor<?> implementor = childTable.getTableLikeImplementor();
            if (implementor instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                if (tableImplementor.getJoinType() != JoinType.INNER ||
                        tableImplementor.getWeakJoinHandle() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    static void renderUsedJoinsAsFrom(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
        }
    }

    static void renderUsedJoinsNormally(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderTo(builder, false);
        }
    }

    static void renderDeeperJoinsAsFrom(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.DEEPER_JOIN_ONLY);
        }
    }

    static void renderUsedJoinConditions(SqlBuilder builder, TableImplementor<?> table) {
        for (RealTable child : table.realTable(builder.getAstContext())) {
            child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
        }
    }

    static void renderId(SqlBuilder builder, TableImplementor<?> table, ImmutableType targetType) {
        ColumnDefinition definition = targetType
                .getIdProp()
                .getStorage(builder.sqlClient().getMetadataStrategy());
        String alias = MutationRender.alias(builder, table);
        if (definition.size() == 1) {
            builder.definition(alias, definition);
            return;
        }
        builder.enter(SqlBuilder.ScopeType.TUPLE);
        for (String columnName : definition) {
            builder.separator();
            builder.sql(alias).sql(".").sql(columnName);
        }
        builder.leave();
    }

    static void renderJoinedTypeBranchJoin(SqlBuilder builder, TableImplementor<?> table) {
        builder.join(JoinType.INNER);
        renderJoinedTypeBranchFrom(builder, table);
        builder.on();
        renderJoinedTypeBranchCondition(builder, table);
    }

    static void renderJoinedTypeBranchFrom(SqlBuilder builder, TableImplementor<?> table) {
        builder
                .sql(table.getImmutableType().getTableName(builder.sqlClient().getMetadataStrategy()))
                .sql(" ")
                .sql(joinedTypeBranchAlias(builder, table));
    }

    static void renderJoinedTypeBranchCondition(SqlBuilder builder, TableImplementor<?> table) {
        InheritanceInfo inheritanceInfo = table.getImmutableType().getInheritanceInfo();
        ImmutableType rootType = inheritanceInfo.getRootType();
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        ColumnDefinition rootDefinition = rootType.getIdProp().getStorage(strategy);
        ColumnDefinition branchDefinition = table.getImmutableType().getIdProp().getStorage(strategy);
        String rootAlias = MutationRender.alias(builder, table);
        String branchAlias = joinedTypeBranchAlias(builder, table);
        int size = rootDefinition.size();
        builder.enter(SqlBuilder.ScopeType.AND);
        for (int i = 0; i < size; i++) {
            builder
                    .separator()
                    .sql(rootAlias)
                    .sql(".")
                    .sql(rootDefinition.name(i))
                    .sql(" = ")
                    .sql(branchAlias)
                    .sql(".")
                    .sql(branchDefinition.name(i));
        }
        builder.leave();
    }

    static String joinedTypeBranchAlias(SqlBuilder builder, TableImplementor<?> table) {
        return TableImplementor.joinedTypeBranchAlias(builder, table);
    }

    static String joinedTypeStageAlias(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType stageType,
            ImmutableType physicalType
    ) {
        ImmutableType type = table.getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (stageType == inheritanceInfo.getRootType() && physicalType == type) {
            return builder.getAstContext().getTableAliasScope().allocateTableAlias(table);
        }
        if (stageType == type) {
            return joinedTypeBranchAlias(builder, table);
        }
        String alias = MutationRender.alias(builder, table);
        return alias +
                (alias.endsWith("_") ? "_" : "__") +
                stageType.getJavaClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    static void renderJoinedTypeStageJoin(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType targetStageType,
            ImmutableType stageType,
            String stageAlias
    ) {
        builder.join(JoinType.INNER);
        renderJoinedTypeStageFrom(builder, stageType, stageAlias);
        builder.on();
        renderJoinedTypeStageCondition(builder, table, targetStageType, stageType, stageAlias);
    }

    static void renderJoinedTypeStageFrom(SqlBuilder builder, ImmutableType stageType, String stageAlias) {
        builder
                .sql(stageType.getTableName(builder.sqlClient().getMetadataStrategy()))
                .sql(" ")
                .sql(stageAlias);
    }

    static void renderJoinedTypeStageCondition(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType targetStageType,
            ImmutableType stageType,
            String stageAlias
    ) {
        MetadataStrategy strategy = builder.sqlClient().getMetadataStrategy();
        ColumnDefinition targetDefinition = targetStageType.getIdProp().getStorage(strategy);
        ColumnDefinition stageDefinition = stageType.getIdProp().getStorage(strategy);
        String targetAlias = MutationRender.alias(builder, table);
        int size = targetDefinition.size();
        builder.enter(SqlBuilder.ScopeType.AND);
        for (int i = 0; i < size; i++) {
            builder
                    .separator()
                    .sql(targetAlias)
                    .sql(".")
                    .sql(targetDefinition.name(i))
                    .sql(" = ")
                    .sql(stageAlias)
                    .sql(".")
                    .sql(stageDefinition.name(i));
        }
        builder.leave();
    }
}
