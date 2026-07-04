package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collections;
import java.util.Map;

final class MutationQuerySupport {

    private MutationQuerySupport() {}

    static MutableRootQueryImpl<TableEx<?>> createUpdateQuery(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType,
            ImplicitDiscriminatorPredicatePolicy discriminatorPredicatePolicy
    ) {
        return new MutationRootQueryImpl(
                new StatementContext(ExecutionPurpose.UPDATE),
                sqlClient,
                immutableType,
                discriminatorPredicatePolicy
        );
    }

    static MutableRootQueryImpl<TableEx<?>> createUpdateQuery(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table,
            ImplicitDiscriminatorPredicatePolicy discriminatorPredicatePolicy
    ) {
        return new MutationRootQueryImpl(
                new StatementContext(ExecutionPurpose.UPDATE),
                sqlClient,
                table,
                discriminatorPredicatePolicy
        );
    }

    static void renderIdInSubQuery(
            SqlBuilder builder,
            MutableRootQueryImpl<TableEx<?>> query,
            TableImplementor<?> table,
            ImmutableType targetType
    ) {
        renderIdInSubQuery(builder, query, table, targetType, Collections.emptyMap());
    }

    static void renderIdInSubQuery(
            SqlBuilder builder,
            MutableRootQueryImpl<TableEx<?>> query,
            TableImplementor<?> table,
            ImmutableType targetType,
            Map<ImmutableType, String> joinedStageAliasMap
    ) {
        MutationJoinRenderSupport.renderId(builder, table, targetType);
        builder.sql(" in ");
        ConfigurableRootQuery<TableEx<?>, Object> idQuery = query
                .select(table.get(table.getImmutableType().getIdProp()))
                .distinct();
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        boolean joinedTypeContextPushed = pushJoinedTypeContext(builder, table, joinedStageAliasMap);
        try {
            ((Ast) idQuery).renderTo(builder);
        } finally {
            if (joinedTypeContextPushed) {
                popJoinedTypeContext(builder);
            }
            builder.leave();
        }
    }

    private static boolean pushJoinedTypeContext(
            SqlBuilder builder,
            TableImplementor<?> table,
            Map<ImmutableType, String> joinedStageAliasMap
    ) {
        InheritanceInfo inheritanceInfo = table.getImmutableType().getInheritanceInfo();
        if (inheritanceInfo == null ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                joinedStageAliasMap.isEmpty()) {
            return false;
        }
        AstContext astContext = builder.getAstContext();
        astContext.pushJoinedTypeBranchUpdate(
                table,
                inheritanceInfo.getRootType(),
                MutationRender.alias(builder, table),
                joinedStageAliasMap
        );
        astContext.pushJoinedTypeBranchTable(table);
        return true;
    }

    private static void popJoinedTypeContext(SqlBuilder builder) {
        AstContext astContext = builder.getAstContext();
        astContext.popJoinedTypeBranchTable();
        astContext.popJoinedTypeBranchUpdate();
    }

    interface ImplicitDiscriminatorPredicatePolicy {

        boolean shouldApply(TableImplementor<?> table);
    }

    private static class MutationRootQueryImpl extends MutableRootQueryImpl<TableEx<?>> {

        private final ImplicitDiscriminatorPredicatePolicy discriminatorPredicatePolicy;

        MutationRootQueryImpl(
                StatementContext ctx,
                JSqlClientImplementor sqlClient,
                ImmutableType immutableType,
                ImplicitDiscriminatorPredicatePolicy discriminatorPredicatePolicy
        ) {
            super(ctx, sqlClient, immutableType);
            this.discriminatorPredicatePolicy = discriminatorPredicatePolicy;
        }

        MutationRootQueryImpl(
                StatementContext ctx,
                JSqlClientImplementor sqlClient,
                TableProxy<?> table,
                ImplicitDiscriminatorPredicatePolicy discriminatorPredicatePolicy
        ) {
            super(ctx, sqlClient, table);
            this.discriminatorPredicatePolicy = discriminatorPredicatePolicy;
        }

        @Override
        protected boolean shouldApplyImplicitDiscriminatorPredicate(TableImplementor<?> table) {
            return discriminatorPredicatePolicy.shouldApply(table);
        }
    }
}
