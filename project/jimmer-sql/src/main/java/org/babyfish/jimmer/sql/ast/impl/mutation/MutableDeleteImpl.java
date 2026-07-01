package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.InheritanceInfo;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinedTableDissociateAction;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageCollector;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsages;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MutableDeleteImpl
        extends AbstractMutableStatementImpl
        implements MutableDelete {

    private final MutableRootQueryImpl<TableEx<?>> deleteQuery;

    private boolean isDissociationDisabled;

    private DeleteMode mode;

    private TypeMatchMode typeMatchMode = TypeMatchMode.AUTO;

    private boolean typeMatchPredicateApplied;

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

    @SuppressWarnings("unchecked")
    private Integer executeImpl(Connection con) {

        JSqlClientImplementor sqlClient = getSqlClient();
        if (getSqlClient().isTargetTransferable()) {
            Executor.validateMutationConnection(con);
        }

        TableImplementor<?> table = getTableLikeImplementor();

        AstContext astContext = new AstContext(sqlClient);
        deleteQuery.applyVirtualPredicates(astContext);
        deleteQuery.applyGlobalFilters(astContext, getContext().getFilterLevel(), null);
        applyTypeMatchPredicate();

        deleteQuery.freeze(astContext);
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
        } finally {
            astContext.popStatement();
        }

        boolean logicalDeleted;
        switch (mode) {
            case PHYSICAL:
                logicalDeleted = false;
                break;
            case LOGICAL:
                if (table.getImmutableType().getLogicalDeletedInfo() == null) {
                    throw new ExecutionException(
                            "The mode of the delete statement cannot be \"" +
                                    DeleteMode.LOGICAL.name() +
                                    "\" because the deleted entity type \"" +
                                    table.getImmutableType() +
                                    "\" does not support logical deleted"
                    );
                }
                logicalDeleted = true;
                break;
            default:
                logicalDeleted = table.getImmutableType().getLogicalDeletedInfo() != null;
                break;
        }

        boolean binLogOnly = sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY;
        DissociationInfo info = sqlClient.getEntityManager().getDissociationInfo(table.getImmutableType());
        boolean directly = table
                .isEmpty(it -> astContext
                        .getTableUsedState(it.realTable(astContext)) == TableUsedState.USED
                ) && binLogOnly && (
                        isDissociationDisabled ||
                                info == null ||
                                info.isDirectlyDeletable(sqlClient.getMetadataStrategy()
                )
        ) && isDirectlyDeletableByInheritance(logicalDeleted);

        if (directly) {
            astContext.pushStatement(this);
            try {
                return executeDirectly(con, astContext, logicalDeleted);
            } finally {
                astContext.popStatement();
            }
        }

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

    private boolean isDirectlyDeletableByInheritance(boolean logicalDeleted) {
        ImmutableType type = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null || logicalDeleted) {
            return true;
        }
        if (inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            return true;
        }
        if (type != inheritanceInfo.getRootType()) {
            return false;
        }
        return inheritanceInfo.getJoinedTableDissociateAction() != JoinedTableDissociateAction.DELETE ||
                TypeMatchModes.resolve(type, typeMatchMode) == TypeMatchMode.EXACT;
    }

    private int executeDirectly(Connection con, AstContext astContext, boolean logicalDeleted) {
        SqlBuilder builder = new SqlBuilder(astContext);
        renderDirectly(builder, logicalDeleted);
        return executeDirectSql(con, builder);
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
    private void renderDirectly(SqlBuilder builder, boolean logicalDeleted) {
        Predicate predicate = deleteQuery.getPredicate(builder.getAstContext());
        TableImplementor<?> table = getTableLikeImplementor();
        if (logicalDeleted) {
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
            builder.sql("delete");
            if (getSqlClient().getDialect().isDeletedAliasRequired()) {
                builder.sql(" ").sql(MutationRender.alias(builder, table));
            }
            builder.from().sql(table.getImmutableType().getTableName(getSqlClient().getMetadataStrategy()));
            if (getSqlClient().getDialect().isDeleteNeedsAsKeyword()) {
                builder.sql(" as ");
            } else {
                builder.sql(" ");
            }
            builder.sql(MutationRender.alias(builder, table));
            if (predicate != null) {
                builder.enter(SqlBuilder.ScopeType.WHERE);
                ((Ast) predicate).renderTo(builder);
                builder.leave();
            }
        }
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
