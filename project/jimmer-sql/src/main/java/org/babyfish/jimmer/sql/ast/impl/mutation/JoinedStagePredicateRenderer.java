package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageCollector;
import org.babyfish.jimmer.sql.ast.impl.table.RealTable;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class JoinedStagePredicateRenderer {

    private final AbstractMutableStatementImpl statement;

    private final TableImplementor<?> table;

    private final ImmutableType type;

    private final ImmutableType targetType;

    private final Map<ImmutableType, String> stageAliasMap;

    JoinedStagePredicateRenderer(
            AbstractMutableStatementImpl statement,
            TableImplementor<?> table,
            ImmutableType targetType,
            Map<ImmutableType, String> stageAliasMap
    ) {
        this.statement = statement;
        this.table = table;
        this.type = table.getImmutableType();
        this.targetType = targetType;
        this.stageAliasMap = stageAliasMap;
    }

    boolean isSupported(Predicate predicate) {
        if (CompositePredicate.isAnd(predicate) || CompositePredicate.isOr(predicate)) {
            for (Predicate child : CompositePredicate.predicates(predicate)) {
                if (!isSupported(child)) {
                    return false;
                }
            }
            return true;
        }
        return isAtomicSupported(predicate);
    }

    void render(Predicate predicate, SqlBuilder builder) {
        if (CompositePredicate.isAnd(predicate) || CompositePredicate.isOr(predicate)) {
            renderComposite(predicate, builder);
        } else {
            renderAtomic(predicate, builder);
        }
    }

    private void renderComposite(Predicate predicate, SqlBuilder builder) {
        int precedence = ((ExpressionImplementor<?>) predicate).precedence();
        builder.enter(
                CompositePredicate.isAnd(predicate) ?
                        SqlBuilder.ScopeType.AND :
                        SqlBuilder.ScopeType.OR
        );
        for (Predicate child : CompositePredicate.predicates(predicate)) {
            builder.separator();
            renderChild(child, precedence, builder);
        }
        builder.leave();
    }

    private void renderChild(Predicate predicate, int parentPrecedence, SqlBuilder builder) {
        if (((ExpressionImplementor<?>) predicate).precedence() <= parentPrecedence) {
            render(predicate, builder);
        } else {
            builder.sql("(").space('\n');
            render(predicate, builder);
            builder.space('\n').sql(")");
        }
    }

    private boolean isAtomicSupported(Predicate predicate) {
        Usage usage = analyze(predicate);
        return !usage.ordinaryTableUsed && usage.stageTypes.size() <= 1;
    }

    private void renderAtomic(Predicate predicate, SqlBuilder builder) {
        Usage usage = analyze(predicate);
        if (usage.stageTypes.isEmpty()) {
            ((Ast) predicate).renderTo(builder);
            return;
        }
        renderExists(usage.stageTypes.iterator().next(), predicate, builder);
    }

    private void renderExists(ImmutableType stageType, Predicate predicate, SqlBuilder builder) {
        String alias = stageAliasMap.get(stageType);
        if (alias == null) {
            alias = MutationJoinRenderSupport.joinedTypeStageAlias(
                    builder,
                    table,
                    stageType,
                    targetType
            );
        }
        builder.sql("exists");
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        builder.sql("select 1 from ");
        MutationJoinRenderSupport.renderJoinedTypeStageFrom(builder, stageType, alias);
        builder.enter(SqlBuilder.ScopeType.WHERE);
        MutationJoinRenderSupport.renderJoinedTypeStageCondition(
                builder,
                table,
                targetType,
                stageType,
                alias
        );
        builder.separator();
        ((Ast) predicate).renderTo(builder);
        builder.leave();
        builder.leave();
    }

    private Usage analyze(Predicate predicate) {
        AstContext astContext = new AstContext(statement.getSqlClient());
        astContext.pushStatement(statement);
        try {
            UsageCollector visitor = new UsageCollector(astContext);
            ((Ast) predicate).accept(visitor);
            return visitor.toUsage();
        } finally {
            astContext.popStatement();
        }
    }

    private class UsageCollector extends TableUsageCollector {

        private final Set<ImmutableType> stageTypes = new LinkedHashSet<>();

        private boolean ordinaryTableUsed;

        UsageCollector(AstContext astContext) {
            super(astContext);
        }

        @Override
        public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
            super.visitTableReference(table, prop, rawId);
            if (!collectJoinedStage(table, prop) && isStatementTable(table)) {
                ordinaryTableUsed = true;
            }
        }

        Usage toUsage() {
            return new Usage(stageTypes, ordinaryTableUsed);
        }

        private boolean collectJoinedStage(RealTable realTable, @Nullable ImmutableProp prop) {
            if (!table.isJoinedTypeBranchRoot()) {
                return false;
            }
            if (realTable.getTableLikeImplementor() == table) {
                return collectJoinedStage(prop);
            }
            for (RealTable current = realTable; current.getParent() != null; current = current.getParent()) {
                if (current.getParent().getTableLikeImplementor() == table &&
                        current.getTableLikeImplementor() instanceof TableImplementor<?>) {
                    ImmutableProp joinProp = ((TableImplementor<?>) current.getTableLikeImplementor()).getJoinProp();
                    return collectJoinedStage(joinProp);
                }
            }
            return false;
        }

        private boolean collectJoinedStage(@Nullable ImmutableProp prop) {
            if (prop == null) {
                return false;
            }
            if (prop.isId() || prop.toOriginal().isId()) {
                return true;
            }
            ImmutableType stageType = TableImplementor.joinedStageType(prop, type);
            if (stageType == null) {
                return false;
            }
            if (stageType != targetType) {
                stageTypes.add(stageType);
            }
            return true;
        }

        private boolean isStatementTable(RealTable table) {
            for (RealTable current = table; current != null; current = current.getParent()) {
                TableLikeImplementor<?> implementor = current.getTableLikeImplementor();
                if (implementor.getStatement() == statement) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class Usage {

        final Set<ImmutableType> stageTypes;

        final boolean ordinaryTableUsed;

        Usage(Collection<ImmutableType> stageTypes, boolean ordinaryTableUsed) {
            this.stageTypes = new LinkedHashSet<>(stageTypes);
            this.ordinaryTableUsed = ordinaryTableUsed;
        }
    }
}
