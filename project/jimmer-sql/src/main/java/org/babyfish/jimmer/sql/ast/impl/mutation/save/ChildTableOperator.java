package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.MutableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.query.MutableQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

class ChildTableOperator extends AbstractOperator {

    private final DeleteContext ctx;

    private final ChildTableOperator parent;

    private final int mutationSubQueryDepth;

    private final QueryReason queryReason;

    private final DisconnectingType disconnectingType;

    private final String tableName;

    private final List<ValueGetter> sourceGetters;

    private final List<ValueGetter> targetGetters;

    private final List<ChildTableOperator> subOperators;

    ChildTableOperator(DeleteContext ctx) {
        this(null, ctx);
    }

    private ChildTableOperator(ChildTableOperator parent, ImmutableProp backReferenceProp) {
        this(parent, parent.ctx.backReferenceOf(backReferenceProp));
    }

    private ChildTableOperator(ChildTableOperator parent, DeleteContext ctx) {
        super(ctx.options.getSqlClient(), ctx.con);
        DissociateAction dissociateAction = ctx.options.getDissociateAction(ctx.path.getBackReferenceProp());
        DisconnectingType disconnectingType;
        if (parent == null) {
            disconnectingType = DisconnectingType.NONE;
        } switch (dissociateAction) {
            case CHECK:
                disconnectingType = DisconnectingType.CHECKING;
                break;
            case SET_NULL:
                disconnectingType = DisconnectingType.SET_NULL;
                break;
            case DELETE:
                ColumnDefinition definition = ctx.backReferenceProp.getStorage(
                        ctx.options.getSqlClient().getMetadataStrategy()
                );
                if (definition.isForeignKey() || ctx.path.getType().getLogicalDeletedInfo() == null) {
                    disconnectingType = DisconnectingType.PHYSICAL_DELETE;
                } else {
                    disconnectingType = DisconnectingType.LOGICAL_DELETE;
                }
                break;
            default:
                disconnectingType = DisconnectingType.NONE;
                break;
        }
        QueryReason queryReason = QueryReason.NONE;
        ImmutableProp prop = ctx.path.getProp();
        if (prop != null && prop.getDeclaringType().isAssignableFrom(prop.getTargetType())) {
            queryReason = QueryReason.RECURSIVE;
        } else if (ctx.trigger != null) {
            queryReason = QueryReason.TRIGGER;
        } else if (disconnectingType == DisconnectingType.CHECKING) {
            queryReason = QueryReason.CHECKING;
        } else if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            Filter<?> filter = ctx.options.getSqlClient().getFilters().getTargetFilter(ctx.path.getProp());
            if (FilterManager.hasUserFilter(filter)) {
                queryReason = QueryReason.FILTER;
            }
        }
        int mutationSubQueryDepth = 0;
        if (parent != null && queryReason == QueryReason.NONE) {
            mutationSubQueryDepth = parent.mutationSubQueryDepth + 1;
            if (mutationSubQueryDepth >= ctx.options.getSqlClient().getMaxMutationSubQueryDepth()) {
                mutationSubQueryDepth = 0;
                queryReason = QueryReason.RECURSIVE;
            }
        }
        this.ctx = ctx;
        this.parent = parent;
        this.mutationSubQueryDepth = mutationSubQueryDepth;
        this.queryReason = queryReason;
        this.disconnectingType = disconnectingType;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.sourceGetters = ValueGetter.valueGetters(sqlClient, ctx.backReferenceProp);
        this.targetGetters = ValueGetter.valueGetters(sqlClient, ctx.path.getType().getIdProp());
        List<ChildTableOperator> subOperators = new ArrayList<>();
        if (ctx.path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backReferenceProp : sqlClient.getEntityManager().getAllBackProps(ctx.path.getType())) {
                if (backReferenceProp.isColumnDefinition() && disconnectingType != DisconnectingType.NONE) {
                    subOperators.add(new ChildTableOperator(this, backReferenceProp));
                }
            }
        }
        this.subOperators = subOperators.isEmpty() ? Collections.emptyList() : subOperators;
    }

    int disconnectExcept(IdPairs idPairs) {
        return disconnectExcept(idPairs, this);
    }

    private int disconnect(Collection<Object> ids, ChildTableOperator caller) {
        if (ids.isEmpty()) {
            return 0;
        }
        if (caller != this) {
            List<Object> preExecutedIds = preDisconnect(ids, null, caller);
            if (preExecutedIds != null) {
                return disconnect(preExecutedIds, this);
            }
        }
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnect(ids, caller);
        }
        return disconnectImpl(ids);
    }

    private int disconnectExcept(IdPairs idPairs, ChildTableOperator caller) {
        if (idPairs.isEmpty()) {
            return 0;
        }
        List<Object> preExecutedIds = preDisconnect(null, idPairs, caller);
        if (preExecutedIds != null) {
            return disconnect(preExecutedIds, this);
        }
        for (ChildTableOperator subOperator : subOperators) {
            subOperator.disconnectExcept(idPairs, caller);
        }
        return disconnectExceptImpl(idPairs);
    }

    private int disconnectImpl(Collection<Object> ids) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperationHead(builder);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, ids, null);
        builder.leave();
        return execute(builder);
    }

    private List<Object> preDisconnect(
            Collection<Object> deletedIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        if (queryReason == QueryReason.NONE) {
            return null;
        }
        MutationTrigger trigger = ctx.trigger;
        if (trigger != null) {
            List<Tuple2<Object, Object>> tuples = findDisconnectingTuplesExcept(
                    deletedIds,
                    retainedIdPairs,
                    caller
            );
            List<Object> targetIds = new ArrayList<>(tuples.size());
            for (Tuple2<Object, Object> tuple : tuples) {
                trigger.deleteMiddleTable(ctx.path.getProp(), tuple.get_1(), tuple.get_2());
                targetIds.add(tuple.get_2());
            }
            return targetIds;
        }
        return findDisconnectingIds(deletedIds, retainedIdPairs, caller);
    }

    private int disconnectExceptImpl(IdPairs idPairs) {
        if (targetGetters.size() == 1 && sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
            return disconnectExceptByBatch(idPairs);
        }
        return disconnectExceptByInPredicate(idPairs);
    }

    private int disconnectExceptByBatch(IdPairs idPairs) {
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        addOperationHead(builder);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, null, idPairs);
        builder.leave();
        return execute(builder, idPairs.entries());
    }

    private int disconnectExceptByInPredicate(IdPairs idPairs) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperationHead(builder);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, null, idPairs);
        builder.leave();
        return execute(builder);
    }

    private void addOperationHead(
            AbstractSqlBuilder<?> builder
    ) {
        if (disconnectingType == DisconnectingType.PHYSICAL_DELETE) {
            builder.sql("delete from ").sql(tableName);
        } else if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            assert logicalDeletedInfo != null;
            builder.sql("update ")
                    .sql(tableName)
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .logicalDeleteAssignment(logicalDeletedInfo, null)
                    .leave();
        } else {
            builder.sql("update ")
                    .sql(tableName)
                    .enter(AbstractSqlBuilder.ScopeType.SET);
            for (ValueGetter sourceGetter : sourceGetters) {
                builder.separator()
                        .sql(sourceGetter)
                        .sql(" = null");
            }
            builder.leave();
        }
    }

    private void addPredicates(
            AbstractSqlBuilder<?> builder,
            Collection<?> deletedIds,
            IdPairs retainedIdPairs
    ) {
        if (builder instanceof BatchSqlBuilder) {
            addPredicatesImpl(
                    (BatchSqlBuilder) builder,
                    deletedIds
            );
        }else {
            addPredicatesImpl(
                    (SqlBuilder) builder,
                    deletedIds,
                    retainedIdPairs
            );
        }
        if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            assert logicalDeletedInfo != null;
            builder.sql(" and ").logicalDeleteFilter(logicalDeletedInfo, null);
        }
    }

    private void addPredicatesImpl(
            BatchSqlBuilder builder,
            Collection<?> deletedIds
    ) {
        if (parent != null) {
            builder.enter(
                    sourceGetters.size() == 1 ?
                            AbstractSqlBuilder.ScopeType.NULL :
                            AbstractSqlBuilder.ScopeType.TUPLE
            );
            for (ValueGetter sourceGetter : sourceGetters) {
                builder.separator();
                builder.sql(sourceGetter);
            }
            builder.leave();
            builder.sql(" in ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
            List<ValueGetter> parentGetters = ValueGetter.valueGetters(
                    builder.sqlClient(),
                    parent.ctx.path.getType().getIdProp()
            );
            for (ValueGetter parentGetter : parentGetters) {
                builder.separator().sql(parentGetter);
            }
            builder.leave();
            builder.sql(" from ").sql(parent.tableName);
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            parent.addPredicatesImpl(builder, deletedIds);
            builder.leave();
            builder.leave();
            return;
        }

        builder.separator();

        if (deletedIds != null) {
            builder.enter(
                    targetGetters.size() == 1 ?
                            AbstractSqlBuilder.ScopeType.NULL :
                            AbstractSqlBuilder.ScopeType.AND
            );
            for (ValueGetter targetGetter : targetGetters) {
                builder.separator();
                builder.sql(targetGetter)
                        .sql(targetGetter).sql(" = ")
                        .variable(targetGetter);
            }
            builder.leave();
            return;
        }
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                sourceGetters,
                targetGetters
        );
    }

    private void addPredicatesImpl(
            SqlBuilder builder,
            Collection<?> deletedIds,
            IdPairs retainedIdPairs
    ) {
        if (parent != null) {
            builder.enter(
                    sourceGetters.size() == 1 ?
                            AbstractSqlBuilder.ScopeType.NULL :
                            AbstractSqlBuilder.ScopeType.TUPLE
            );
            for (ValueGetter sourceGetter : sourceGetters) {
                builder.separator();
                builder.sql(sourceGetter);
            }
            builder.sql(" in ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
            List<ValueGetter> parentGetters = ValueGetter.valueGetters(
                    builder.sqlClient(),
                    parent.ctx.path.getType().getIdProp()
            );
            for (ValueGetter parentGetter : parentGetters) {
                builder.separator().sql(parentGetter);
            }
            builder.leave();
            builder.sql(" from ").sql(parent.tableName);
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            parent.addPredicatesImpl(builder, deletedIds, retainedIdPairs);
            builder.leave();
            builder.leave();
            builder.leave();
            return;
        }

        builder.separator();

        if (deletedIds != null) {
            ComparisonPredicates.renderIn(
                    false,
                    targetGetters,
                    deletedIds,
                    builder
            );
            return;
        }

        if (retainedIdPairs.entries().size() == 1) {
            Tuple2<Object, Collection<Object>> tuple = retainedIdPairs.entries().iterator().next();
            ComparisonPredicates.renderEq(
                    false,
                    sourceGetters,
                    tuple.get_1(),
                    builder
            );
            if (!tuple.get_2().isEmpty()) {
                builder.separator();
                ComparisonPredicates.renderIn(
                        true,
                        targetGetters,
                        tuple.get_2(),
                        builder
                );
            }
            return;
        }
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                sourceGetters,
                targetGetters,
                retainedIdPairs
        );
    }

    private List<Tuple2<Object, Object>> findDisconnectingTuplesExcept(
            Collection<Object> deletedIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, deletedIds, retainedIdPairs, caller);
        return query.select(
                query.getTableImplementor().getAssociatedId(ctx.backReferenceProp),
                query.getTableImplementor().getId()
        ).execute(con);
    }

    private List<Object> findDisconnectingIds(
            Collection<Object> deleteIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, deleteIds, retainedIdPairs, caller);
        return query.select(
                query.getTableImplementor().getId()
        ).execute(con);
    }

    private void addDisconnectingConditions(
            AbstractMutableQueryImpl query,
            Collection<Object> deletedIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        TableImplementor<?> table = query.getTableImplementor();
        if (this != caller) {
            MutableSubQueryImpl subQuery = new MutableSubQueryImpl(query, parent.ctx.path.getType());
            TableImplementor<?> parentTable = subQuery.getTableImplementor();
            parent.addDisconnectingConditions(
                    subQuery,
                    deletedIds,
                    retainedIdPairs,
                    caller
            );
            query.where(
                    table.getAssociatedId(ctx.backReferenceProp).in(
                            subQuery.select(parentTable.getId())
                    )
            );
            return;
        }
        if (deletedIds != null) {
            if (!deletedIds.isEmpty()) {
                query.where(table.getId().in(deletedIds));
            }
            return;
        }
        if (retainedIdPairs.entries().size() == 1) {
            query.where(
                    table.getAssociatedId(ctx.backReferenceProp).in(
                            Tuple2.projection1(retainedIdPairs.entries())
                    )
            );
            if (!retainedIdPairs.tuples().isEmpty()) {
                query.where(
                        table.getId().notIn(
                                Tuple2.projection2(retainedIdPairs.tuples())
                        )
                );
            }
            return;
        }
        query.where(
                table.getAssociatedId(ctx.backReferenceProp).in(
                        Tuple2.projection1(retainedIdPairs.entries())
                )
        );
        if (!retainedIdPairs.tuples().isEmpty()) {
            query.where(
                    Expression.tuple(
                            table.getAssociatedId(ctx.backReferenceProp),
                            table.getId()
                    ).notIn(retainedIdPairs.tuples())
            );
        }
    }

    private enum DisconnectingType {
        CHECKING,
        NONE,
        SET_NULL,
        LOGICAL_DELETE,
        PHYSICAL_DELETE;
        boolean isDelete() {
            return this == LOGICAL_DELETE || this == PHYSICAL_DELETE;
        }
    }
}
