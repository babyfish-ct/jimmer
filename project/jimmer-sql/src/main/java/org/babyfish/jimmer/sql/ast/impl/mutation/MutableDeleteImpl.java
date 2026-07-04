package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDissociateAction;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageCollector;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsages;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.DeleteJoin;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.util.*;

public class MutableDeleteImpl
        extends AbstractMutableStatementImpl
        implements MutableDelete {

    private final MutableRootQueryImpl<TableEx<?>> deleteQuery;

    private boolean isDissociationDisabled;

    private DeleteMode mode;

    private TypeMatchMode typeMatchMode = TypeMatchMode.AUTO;

    private boolean typeMatchPredicateApplied;

    private enum DirectRenderMode {
        NONE,
        PLAIN,
        DELETE_JOIN,
        UPDATE_JOIN,
        ID_SUB_QUERY
    }

    public MutableDeleteImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        deleteQuery = new MutableRootQueryImpl<>(
                new StatementContext(ExecutionPurpose.delete(QueryReason.CANNOT_DELETE_DIRECTLY)),
                sqlClient,
                immutableType
        );
        this.mode = DeleteMode.AUTO;
    }

    public MutableDeleteImpl(JSqlClientImplementor sqlClient, TableProxy<?> table) {
        super(sqlClient, table);
        deleteQuery = new MutableRootQueryImpl<>(
                new StatementContext(ExecutionPurpose.delete(QueryReason.CANNOT_DELETE_DIRECTLY)),
                sqlClient,
                table
        );
        this.mode = DeleteMode.AUTO;
    }

    @Override
    public <T extends TableLike<?>> T getTable() {
        return deleteQuery.getTable();
    }

    @Override
    public TableImplementor<?> getTableLikeImplementor() {
        return (TableImplementor<?>) deleteQuery.getTableLikeImplementor();
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
    }

    @Override
    public StatementContext getContext() {
        return deleteQuery.getContext();
    }

    @Override
    public MutableDelete where(Predicate... predicates) {
        deleteQuery.where(predicates);
        return this;
    }

    @Override
    public void whereByFilter(TableImplementor<?> tableImplementor, List<Predicate> predicates) {
        deleteQuery.whereByFilter(tableImplementor, predicates);
    }

    @Override
    public MutableDelete disableDissociation() {
        isDissociationDisabled = true;
        return this;
    }

    @Override
    public MutableDelete setMode(DeleteMode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public MutableDelete setTypeMatchMode(TypeMatchMode mode) {
        this.typeMatchMode = mode != null ? mode : TypeMatchMode.AUTO;
        return this;
    }

    @Override
    public Integer execute(Connection con) {
        return getSqlClient()
                .getConnectionManager()
                .execute(con, this::executeImpl);
    }

    @Override
    protected void onFrozen(AstContext astContext) {
        deleteQuery.freeze(astContext);
    }

    private void applyTypeMatchPredicate() {
        if (typeMatchPredicateApplied) {
            return;
        }
        typeMatchPredicateApplied = true;
        ImmutableType type = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return;
        }
        TypeMatchMode resolvedMode = TypeMatchModes.resolve(type, typeMatchMode);
        if (resolvedMode == TypeMatchMode.EXACT && !type.isInstantiable()) {
            throw new ExecutionException(
                    "Cannot delete inheritance entity type \"" +
                            type +
                            "\" exactly because it is abstract. Delete an instantiable type or use " +
                            TypeMatchMode.POLYMORPHIC +
                            " type match mode."
            );
        }
        boolean manualPredicateRequired =
                type == inheritanceInfo.getRootType() ||
                        (resolvedMode == TypeMatchMode.EXACT && !type.getAllDerivedTypes().isEmpty());
        if (!manualPredicateRequired) {
            return;
        }
        Collection<ImmutableType> deletedTypes = InheritanceMutationUtils.deletedTypes(
                inheritanceInfo,
                type,
                typeMatchMode
        );
        List<Object> values = InheritanceMutationUtils.discriminatorValues(inheritanceInfo, deletedTypes);
        if (values.isEmpty()) {
            return;
        }
        TableImplementor<?> table = getTableLikeImplementor();
        PropExpression<Object> expr = table.get(inheritanceInfo.getDiscriminatorProp(type), false);
        deleteQuery.where(values.size() == 1 ? expr.eq(values.get(0)) : expr.in(values));
    }

    private Integer executeImpl(Connection con) {
        JSqlClientImplementor sqlClient = getSqlClient();
        if (getSqlClient().isTargetTransferable()) {
            Executor.validateMutationConnection(con);
        }

        TableImplementor<?> table = getTableLikeImplementor();
        DeleteExecution execution = prepareExecution(sqlClient, table);
        if (execution.directRenderMode != DirectRenderMode.NONE) {
            return executeDirectly(
                    con,
                    execution.astContext,
                    execution.logicalDeleted,
                    execution.directRenderMode,
                    execution.joinedTypeBranchTableRequired
            );
        }
        return executeByDeleter(con, sqlClient, table, execution.binLogOnly);
    }

    private DeleteExecution prepareExecution(
            JSqlClientImplementor sqlClient,
            TableImplementor<?> table
    ) {
        AstContext astContext = new AstContext(sqlClient);
        deleteQuery.applyVirtualPredicates(astContext);
        deleteQuery.applyGlobalFilters(astContext, getContext().getFilterLevel(), null);
        applyTypeMatchPredicate();

        deleteQuery.freeze(astContext);
        boolean joinedTypeBranchTableRequired = analyzeTableUsage(astContext, table);
        boolean logicalDeleted = resolveLogicalDeleted(table);

        boolean binLogOnly = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY;
        boolean directlyEligible = binLogOnly &&
                isDirectlyDeletableByDissociation(logicalDeleted) &&
                isDirectlyDeletableByInheritance(logicalDeleted);
        DirectRenderMode directRenderMode = directlyEligible ?
                directRenderMode(astContext, logicalDeleted, joinedTypeBranchTableRequired) :
                DirectRenderMode.NONE;

        return new DeleteExecution(
                astContext,
                logicalDeleted,
                binLogOnly,
                joinedTypeBranchTableRequired,
                directRenderMode
        );
    }

    private boolean analyzeTableUsage(AstContext astContext, TableImplementor<?> table) {
        astContext.pushStatement(deleteQuery);
        try {
            TableUsageCollector visitor = new TableUsageCollector(astContext);
            visitor.visitStatement(this);
            for (Predicate predicate : deleteQuery.unfrozenPredicates()) {
                ((Ast) predicate).accept(visitor);
            }
            TableUsages tableUsages = visitor.toTableUsages();
            tableUsages.applyUsedStatesTo(astContext);
            tableUsages.allocateAndBindAliases(astContext);
            return visitor.isJoinedTypeBranchTableRequired(table);
        } finally {
            astContext.popStatement();
        }
    }

    private boolean resolveLogicalDeleted(TableImplementor<?> table) {
        LogicalDeletedInfo logicalDeletedInfo = table.getImmutableType().getLogicalDeletedInfo();
        switch (mode) {
            case PHYSICAL:
                return false;
            case LOGICAL:
                if (logicalDeletedInfo == null) {
                    throw new ExecutionException(
                            "The mode of the delete statement cannot be \"" +
                                    DeleteMode.LOGICAL.name() +
                                    "\" because the deleted entity type \"" +
                                    table.getImmutableType() +
                                    "\" does not support logical deleted"
                    );
                }
                return true;
            default:
                return logicalDeletedInfo != null;
        }
    }

    @SuppressWarnings("unchecked")
    private int executeByDeleter(
            Connection con,
            JSqlClientImplementor sqlClient,
            TableImplementor<?> table,
            boolean binLogOnly
    ) {
        List<Object> ids = null;
        Collection<ImmutableSpi> rows = null;
        if (binLogOnly) {
            ids = deleteQuery
                    .select(table.get(table.getImmutableType().getIdProp()))
                    .distinct()
                    .execute(con);
            if (ids.isEmpty()) {
                return 0;
            }
        } else {
            rows = (List<ImmutableSpi>) deleteQuery
                    .select(table)
                    .execute(con);
            if (rows.isEmpty()) {
                return 0;
            }
        }
        Deleter deleter = new Deleter(
                table.getImmutableType(),
                new DeleteCommandImpl.OptionsImpl(sqlClient, con, mode, typeMatchMode),
                con,
                binLogOnly ? null : new MutationTrigger(),
                new HashMap<>()
        );
        if (ids != null) {
            deleter.addIds(ids);
        } else {
            deleter.addRows(rows);
        }
        return deleter.execute().getTotalAffectedRowCount();
    }

    private static class DeleteExecution {

        final AstContext astContext;

        final boolean logicalDeleted;

        final boolean binLogOnly;

        final boolean joinedTypeBranchTableRequired;

        final DirectRenderMode directRenderMode;

        DeleteExecution(
                AstContext astContext,
                boolean logicalDeleted,
                boolean binLogOnly,
                boolean joinedTypeBranchTableRequired,
                DirectRenderMode directRenderMode
        ) {
            this.astContext = astContext;
            this.logicalDeleted = logicalDeleted;
            this.binLogOnly = binLogOnly;
            this.joinedTypeBranchTableRequired = joinedTypeBranchTableRequired;
            this.directRenderMode = directRenderMode;
        }
    }

    private boolean isDirectlyDeletableByDissociation(boolean logicalDeleted) {
        if (isDissociationDisabled) {
            return true;
        }
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        for (ImmutableType type : directlyDeletedDissociationTypes(logicalDeleted)) {
            DissociationInfo info = getSqlClient().getEntityManager().getDissociationInfo(type);
            if (info != null && !info.isDirectlyDeletable(strategy)) {
                return false;
            }
        }
        return true;
    }

    private Collection<ImmutableType> directlyDeletedDissociationTypes(boolean logicalDeleted) {
        ImmutableType type = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (logicalDeleted ||
                inheritanceInfo == null ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getJoinedTableDissociateAction() == JoinedTableDissociateAction.DELETE) {
            return Collections.singleton(type);
        }
        Set<ImmutableType> types = new LinkedHashSet<>();
        types.add(inheritanceInfo.getRootType());
        types.addAll(InheritanceMutationUtils.deletedTypes(inheritanceInfo, type, typeMatchMode));
        return types;
    }

    private boolean isDirectlyDeletableByInheritance(boolean logicalDeleted) {
        ImmutableType type = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || logicalDeleted) {
            return true;
        }
        if (inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            return true;
        }
        if (inheritanceInfo.getJoinedTableDissociateAction() != JoinedTableDissociateAction.DELETE) {
            return true;
        }
        return type == inheritanceInfo.getRootType() &&
                TypeMatchModes.resolve(type, typeMatchMode) == TypeMatchMode.EXACT;
    }

    private DirectRenderMode directRenderMode(
            AstContext astContext,
            boolean logicalDeleted,
            boolean joinedTypeBranchTableRequired
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        if (!joinedTypeBranchTableRequired && !MutationJoinRenderSupport.hasUsedChild(table, astContext)) {
            return DirectRenderMode.PLAIN;
        }
        if (logicalDeleted) {
            UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
            if (updateJoin != null &&
                    (
                            updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY ||
                                    updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                                            !MutationJoinRenderSupport.hasFirstLevelJoinUnsupportedByFromOnly(table, astContext)
                    )
            ) {
                return DirectRenderMode.UPDATE_JOIN;
            }
        } else {
            DeleteJoin deleteJoin = getSqlClient().getDialect().getDeleteJoin();
            if (deleteJoin != null &&
                    (deleteJoin.getFrom() != DeleteJoin.From.AS_USING ||
                            !MutationJoinRenderSupport.hasFirstLevelJoinUnsupportedByFromOnly(table, astContext))) {
                return DirectRenderMode.DELETE_JOIN;
            }
        }
        return getSqlClient().getDialect().isTableOfSubQueryMutable() ?
                DirectRenderMode.ID_SUB_QUERY :
                DirectRenderMode.NONE;
    }

    private int executeDirectly(
            Connection con,
            AstContext astContext,
            boolean logicalDeleted,
            DirectRenderMode directRenderMode,
            boolean joinedTypeBranchTableRequired
    ) {
        astContext.pushStatement(deleteQuery);
        if (joinedTypeBranchTableRequired) {
            astContext.pushJoinedTypeBranchTable(getTableLikeImplementor());
        }
        try {
            SqlBuilder builder = new SqlBuilder(astContext);
            renderDirectly(builder, logicalDeleted, directRenderMode, joinedTypeBranchTableRequired);
            return executeDirectSql(con, builder);
        } finally {
            if (joinedTypeBranchTableRequired) {
                astContext.popJoinedTypeBranchTable();
            }
            astContext.popStatement();
        }
    }

    private int executeDirectSql(Connection con, SqlBuilder builder) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return getSqlClient().getExecutor().execute(
                new Executor.Args<>(
                        getSqlClient(),
                        con,
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        sqlResult.get_3(),
                        ExecutionPurpose.delete(QueryReason.NONE),
                        null,
                        null,
                        (stmt, args) -> stmt.executeUpdate()
                )
        );
    }

    @SuppressWarnings("unchecked")
    private void renderDirectly(
            SqlBuilder builder,
            boolean logicalDeleted,
            DirectRenderMode directRenderMode,
            boolean joinedTypeBranchTableRequired
    ) {
        Predicate predicate = deleteQuery.getPredicate(builder.getAstContext());
        TableImplementor<?> table = getTableLikeImplementor();
        if (logicalDeleted) {
            if (directRenderMode == DirectRenderMode.ID_SUB_QUERY) {
                renderLogicalDeleteWithIdSubQuery(builder);
                return;
            }
            if (directRenderMode == DirectRenderMode.UPDATE_JOIN) {
                renderLogicalDeleteWithUpdateJoin(builder, joinedTypeBranchTableRequired);
                return;
            }
            LogicalDeletedInfo logicalDeletedInfo = table.getImmutableType().getLogicalDeletedInfo();
            LogicalDeletedValueGenerator<?> generator =
                    LogicalDeletedValueGenerators.of(logicalDeletedInfo, getSqlClient());
            assert generator != null;
            MutableUpdateImpl update = new MutableUpdateImpl(getSqlClient(), table.getImmutableType());
            update.shareRootAliasWith(deleteQuery.getTableLikeImplementor());
            update.set(
                    (PropExpression<Object>)PropExpressionImpl.of(
                            update.getTable(),
                            deleteQuery.getType().getLogicalDeletedInfo().getProp(),
                            false
                    ),
                    generator.generate()
            );
            update.where(deleteQuery.getPredicate(builder.getAstContext()));
            update.renderTo(builder);
        } else {
            if (directRenderMode == DirectRenderMode.ID_SUB_QUERY) {
                renderPhysicalDeleteWithIdSubQuery(builder);
                return;
            }
            ImmutableType targetType = directDeleteTargetType(false);
            builder.sql("delete");
            DeleteJoin deleteJoin = directRenderMode == DirectRenderMode.DELETE_JOIN ?
                    getSqlClient().getDialect().getDeleteJoin() :
                    null;
            if (getSqlClient().getDialect().isDeletedAliasRequired() ||
                    deleteJoin != null && deleteJoin.getFrom() == DeleteJoin.From.AS_JOIN) {
                builder.sql(" ").sql(MutationRender.alias(builder, table));
            }
            if (deleteJoin != null && deleteJoin.getFrom() == DeleteJoin.From.AS_JOIN) {
                renderDeleteTarget(builder, table, targetType);
                if (joinedTypeBranchTableRequired) {
                    MutationJoinRenderSupport.renderJoinedTypeBranchJoin(builder, table);
                }
                MutationJoinRenderSupport.renderUsedJoinsNormally(builder, table);
            } else {
                renderDeleteTarget(builder, table, targetType);
                if (deleteJoin != null) {
                    renderDeleteUsing(builder, table, joinedTypeBranchTableRequired);
                }
            }
            if (predicate != null || deleteJoin != null && deleteJoin.getFrom() == DeleteJoin.From.AS_USING) {
                builder.enter(SqlBuilder.ScopeType.WHERE);
                if (deleteJoin != null && deleteJoin.getFrom() == DeleteJoin.From.AS_USING) {
                    if (joinedTypeBranchTableRequired) {
                        MutationJoinRenderSupport.renderJoinedTypeBranchCondition(builder, table);
                    }
                    MutationJoinRenderSupport.renderUsedJoinConditions(builder, table);
                }
                if (predicate != null) {
                    builder.separator();
                    ((Ast) predicate).renderTo(builder);
                }
                builder.leave();
            }
        }
    }

    private void renderLogicalDeleteWithUpdateJoin(SqlBuilder builder, boolean joinedTypeBranchTableRequired) {
        TableImplementor<?> table = getTableLikeImplementor();
        Predicate predicate = deleteQuery.getPredicate(builder.getAstContext());
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        ImmutableType type = table.getImmutableType();
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        builder
                .sql("update ")
                .sql(type.getTableName(strategy));
        if (getSqlClient().getDialect().isUpdateNeedsAsKeyword()) {
            builder.sql(" as ");
        } else {
            builder.sql(" ");
        }
        builder.sql(MutationRender.alias(builder, table));
        if (updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
            if (joinedTypeBranchTableRequired) {
                MutationJoinRenderSupport.renderJoinedTypeBranchJoin(builder, table);
            }
            MutationJoinRenderSupport.renderUsedJoinsNormally(builder, table);
        }
        renderLogicalDeletedAssignment(builder, table, updateJoin.isJoinedTableUpdatable());
        if (updateJoin.getFrom() == UpdateJoin.From.AS_JOIN) {
            builder.from().enter(SqlBuilder.ScopeType.COMMA);
            if (joinedTypeBranchTableRequired) {
                MutationJoinRenderSupport.renderJoinedTypeBranchFrom(builder, table);
            }
            MutationJoinRenderSupport.renderUsedJoinsAsFrom(builder, table);
            builder.leave();
            MutationJoinRenderSupport.renderDeeperJoinsAsFrom(builder, table);
        }
        if (predicate != null || updateJoin.getFrom() == UpdateJoin.From.AS_JOIN) {
            builder.enter(SqlBuilder.ScopeType.WHERE);
            if (updateJoin.getFrom() == UpdateJoin.From.AS_JOIN) {
                if (joinedTypeBranchTableRequired) {
                    MutationJoinRenderSupport.renderJoinedTypeBranchCondition(builder, table);
                }
                MutationJoinRenderSupport.renderUsedJoinConditions(builder, table);
            }
            if (predicate != null) {
                builder.separator();
                ((Ast) predicate).renderTo(builder);
            }
            builder.leave();
        }
    }

    private void renderDeleteUsing(
            SqlBuilder builder,
            TableImplementor<?> table,
            boolean joinedTypeBranchTableRequired
    ) {
        builder.sql(" using ").enter(SqlBuilder.ScopeType.COMMA);
        if (joinedTypeBranchTableRequired) {
            MutationJoinRenderSupport.renderJoinedTypeBranchFrom(builder, table);
        }
        MutationJoinRenderSupport.renderUsedJoinsAsFrom(builder, table);
        builder.leave();
        MutationJoinRenderSupport.renderDeeperJoinsAsFrom(builder, table);
    }

    private void renderPhysicalDeleteWithIdSubQuery(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        ImmutableType targetType = directDeleteTargetType(false);
        builder.sql("delete");
        if (getSqlClient().getDialect().isDeletedAliasRequired()) {
            builder.sql(" ").sql(MutationRender.alias(builder, table));
        }
        builder.from().sql(targetType.getTableName(getSqlClient().getMetadataStrategy()));
        if (getSqlClient().getDialect().isDeleteNeedsAsKeyword()) {
            builder.sql(" as ");
        } else {
            builder.sql(" ");
        }
        builder.sql(MutationRender.alias(builder, table));
        builder.enter(SqlBuilder.ScopeType.WHERE);
        renderIdInSubQuery(builder, table, targetType);
        builder.leave();
    }

    private void renderLogicalDeleteWithIdSubQuery(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        ImmutableType type = table.getImmutableType();
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        builder
                .sql("update ")
                .sql(type.getTableName(strategy));
        if (getSqlClient().getDialect().isUpdateNeedsAsKeyword()) {
            builder.sql(" as ");
        } else {
            builder.sql(" ");
        }
        builder.sql(MutationRender.alias(builder, table));
        renderLogicalDeletedAssignment(builder, table, false);
        builder.enter(SqlBuilder.ScopeType.WHERE);
        renderIdInSubQuery(builder, table, type);
        builder.leave();
    }

    private void renderLogicalDeletedAssignment(
            SqlBuilder builder,
            TableImplementor<?> table,
            boolean withTargetPrefix
    ) {
        ImmutableType type = table.getImmutableType();
        LogicalDeletedInfo logicalDeletedInfo = type.getLogicalDeletedInfo();
        LogicalDeletedValueGenerator<?> generator =
                LogicalDeletedValueGenerators.of(logicalDeletedInfo, getSqlClient());
        assert generator != null;
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        builder.enter(SqlBuilder.ScopeType.SET);
        builder.separator();
        if (withTargetPrefix) {
            builder.definition(
                    MutationRender.alias(builder, table),
                    logicalDeletedInfo.getProp().getStorage(strategy)
            );
        } else {
            builder.definition(logicalDeletedInfo.getProp().getStorage(strategy));
        }
        builder.sql(" = ");
        builder.variable(Variables.process(
                generator.generate(),
                logicalDeletedInfo.getProp(),
                getSqlClient()
        ));
        builder.leave();
    }

    private void renderDeleteTarget(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType targetType
    ) {
        builder.from().sql(targetType.getTableName(getSqlClient().getMetadataStrategy()));
        if (getSqlClient().getDialect().isDeleteNeedsAsKeyword()) {
            builder.sql(" as ");
        } else {
            builder.sql(" ");
        }
        builder.sql(MutationRender.alias(builder, table));
    }

    private ImmutableType directDeleteTargetType(boolean logicalDeleted) {
        ImmutableType type = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (!logicalDeleted &&
                inheritanceInfo != null &&
                inheritanceInfo.getStrategy() == InheritanceType.JOINED &&
                inheritanceInfo.getJoinedTableDissociateAction() != JoinedTableDissociateAction.DELETE) {
            return inheritanceInfo.getRootType();
        }
        return type;
    }

    private void renderIdInSubQuery(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType targetType
    ) {
        MutationJoinRenderSupport.renderId(builder, table, targetType);
        builder.sql(" in ");
        ConfigurableRootQuery<TableEx<?>, Object> idQuery = deleteQuery
                .select(table.get(table.getImmutableType().getIdProp()))
                .distinct();
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        ((Ast) idQuery).renderTo(builder);
        builder.leave();
    }

    public static boolean isCompatible(
            AbstractMutableStatementImpl a,
            AbstractMutableStatementImpl b
    ) {
        if (a == b) {
            return true;
        }
        if (a instanceof MutableDeleteImpl) {
            return ((MutableDeleteImpl)a).deleteQuery == b;
        }
        if (b instanceof MutableDeleteImpl) {
            return ((MutableDeleteImpl)b).deleteQuery == a;
        }
        return false;
    }
}
