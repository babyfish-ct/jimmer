package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.impl.FilterManager;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;
import java.util.stream.Collectors;

class ChildTableOperator extends AbstractOperator {

    final DeleteContext ctx;

    private final ChildTableOperator parent;

    final int mutationSubQueryDepth;

    private final QueryReason queryReason;

    final DisconnectingType disconnectingType;

    private final String tableName;

    private final List<ValueGetter> sourceGetters;

    final List<ValueGetter> targetGetters;

    ChildTableOperator(DeleteContext ctx) {
        this(null, ctx);
    }

    private ChildTableOperator(ChildTableOperator parent, ImmutableProp backReferenceProp) {
        this(parent, parent.ctx.backPropOf(backReferenceProp));
    }

    private ChildTableOperator(ChildTableOperator parent, DeleteContext ctx) {
        super(ctx.options.getSqlClient(), ctx.con);
        DissociateAction dissociateAction = ctx.options.getDissociateAction(ctx.path.getBackProp());
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
                ColumnDefinition definition = ctx.backProp.getStorage(
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
        if (ctx.trigger != null) {
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
        if (parent != null) {
            mutationSubQueryDepth = parent.mutationSubQueryDepth + 1;
            if (mutationSubQueryDepth >= ctx.options.getSqlClient().getMaxMutationSubQueryDepth()) {
                mutationSubQueryDepth = 0;
                queryReason = QueryReason.TOO_DEEP;
            }
        }
        this.ctx = ctx;
        this.parent = parent;
        this.mutationSubQueryDepth = mutationSubQueryDepth;
        this.queryReason = queryReason;
        this.disconnectingType = disconnectingType;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.sourceGetters = ValueGetter.valueGetters(sqlClient, ctx.backProp);
        this.targetGetters = ValueGetter.valueGetters(sqlClient, ctx.path.getType().getIdProp());
    }

    final void disconnectExcept(IdPairs idPairs) {
        disconnect(DisconnectionArgs.retain(idPairs, this));
    }

    private void disconnect(DisconnectionArgs args) {
        if (args.isEmpty()) {
            return;
        }
        if (ctx.trigger != null) {
            List<ImmutableSpi> rows = findDisconnectingObjects(args);
            if (rows.isEmpty()) {
                return;
            }
            if (args.deletedIds == null) {
                PropId idPropId = ctx.path.getType().getIdProp().getId();;
                args = DisconnectionArgs.delete(
                        rows
                                .stream()
                                .map(row -> row.__get(idPropId))
                                .collect(Collectors.toList()),
                        this
                );
            }
            MutationTrigger trigger = ctx.trigger;
            if (disconnectingType.isDelete()) {
                for (ImmutableSpi row : rows) {
                    trigger.modifyEntityTable(row, null);
                }
            } else {
                PropId backPropId = ctx.backProp.getId();
                for (ImmutableSpi row : rows) {
                    ImmutableSpi detachedRow = (ImmutableSpi) Internal.produce(
                            ctx.path.getType(),
                            row,
                            draft -> {
                                ((DraftSpi)draft).__set(backPropId, null);
                            }
                    );
                    trigger.modifyEntityTable(row, detachedRow);
                }
            }
        }
        if (args.deletedIds == null || this != args.caller) {
            List<Object> preExecutedIds = preDisconnect(args);
            if (preExecutedIds != null) {
                DisconnectionArgs subArgs = DisconnectionArgs.delete(preExecutedIds, this);
                disconnect(subArgs);
                return;
            }
        }
        for (ChildTableOperator subOperator : subOperators()) {
            subOperator.disconnect(args);
        }
        if (disconnectingType.isDelete()) {
            for (MiddleTableOperator middleTableOperator : middleTableOperators()) {
                middleTableOperator.disconnect(args);
            }
        }
        disconnectImpl(args);
    }

    private List<Object> preDisconnect(DisconnectionArgs args) {
        if (queryReason == QueryReason.NONE) {
            return null;
        }
        return findDisconnectingIds(args);
    }

    private void disconnectImpl(DisconnectionArgs args) {
        if (args.deletedIds != null) {
            SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
            addOperationHead(builder, currentDepth(args));
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            addPredicates(builder, args, currentDepth(args));
            builder.leave();
            int rowCount = execute(builder);
            AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
        } else if (targetGetters.size() == 1 && sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
            disconnectExceptByBatch(args);
        } else {
            disconnectExceptByInPredicate(args);
        }
    }

    private void disconnectExceptByBatch(DisconnectionArgs args) {
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        addOperationHead(builder, currentDepth(args));
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, args, currentDepth(args));
        builder.leave();
        int rowCount = execute(builder, args.retainedIdPairs.entries());
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    private void disconnectExceptByInPredicate(DisconnectionArgs args) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperationHead(builder, currentDepth(args));
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, args, currentDepth(args));
        builder.leave();
        int rowCount = execute(builder);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    private void addOperationHead(
            AbstractSqlBuilder<?> builder,
            int depth
    ) {
        if (disconnectingType == DisconnectingType.PHYSICAL_DELETE) {
            builder.sql("delete from ").sql(tableName);
            if (depth != 0) {
                builder.sql(" ").sql(alias(depth));
            }
        } else if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            assert logicalDeletedInfo != null;
            builder.sql("update ").sql(tableName);
            if (depth != 0) {
                builder.sql(" ").sql(alias(depth));
            }
            builder
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .logicalDeleteAssignment(logicalDeletedInfo, null)
                    .leave();
        } else {
            builder.sql("update ")
                    .sql(tableName);
            if (depth != 0) {
                builder.sql(" ").sql(alias(depth));
            }
            builder.enter(AbstractSqlBuilder.ScopeType.SET);
            for (ValueGetter sourceGetter : sourceGetters) {
                builder.separator()
                        .sql(sourceGetter)
                        .sql(" = null");
            }
            builder.leave();
        }
    }

    final void addPredicates(
            AbstractSqlBuilder<?> builder,
            DisconnectionArgs args,
            int depth
    ) {
        if (builder instanceof BatchSqlBuilder) {
            addPredicatesImpl(
                    (BatchSqlBuilder) builder,
                    args.deletedIds,
                    args.caller,
                    depth
            );
        }else {
            addPredicatesImpl((SqlBuilder) builder, args, depth);
        }
        if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            assert logicalDeletedInfo != null;
            builder.sql(" and ").logicalDeleteFilter(logicalDeletedInfo, alias(depth));
        }
    }

    private void addPredicatesImpl(
            BatchSqlBuilder builder,
            Collection<Object> deletedIds,
            ChildTableOperator caller,
            int depth
    ) {
        if (isJoinAllowed(deletedIds, caller)) {
            builder.sql("exists").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            ChildTableOperator deeper = parent;
            int childDepth = depth + 1;
            builder.sql("select * from ").sql(deeper.tableName).sql(" ").sql(alias(childDepth));
            while (deeper.isJoinAllowed(deletedIds, caller)) {
                builder.sql(" inner join ").sql(deeper.parent.tableName).sql(" ").sql(alias(++childDepth)).sql(" on ");
                deeper.addJoinPredicates(builder, childDepth);
                deeper = deeper.parent;
            }
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            addJoinPredicates(builder, depth + 1);
            deeper.addPredicatesImpl(builder, deletedIds, caller, childDepth);
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
            DisconnectionArgs args,
            int depth
    ) {
        if (isJoinAllowed(args.deletedIds, args.caller)) {
            builder.sql("exists").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
            ChildTableOperator deeper = parent;
            int childDepth = depth + 1;
            builder.sql("select * from ").sql(deeper.tableName).sql(" ").sql(alias(childDepth));
            while (deeper.isJoinAllowed(args.deletedIds, args.caller)) {
                builder.sql(" inner join ").sql(deeper.parent.tableName).sql(" ").sql(alias(++childDepth)).sql(" on ");
                deeper.addJoinPredicates(builder, childDepth);
                deeper = deeper.parent;
            }
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            addJoinPredicates(builder, depth + 1);
            deeper.addPredicatesImpl(builder, args, childDepth);
            builder.leave();
            builder.leave();
            return;
        }

        builder.separator();

        String alias = alias(depth);
        Collection<Object> deletedIds = args.deletedIds;
        if (deletedIds != null) {
            ComparisonPredicates.renderIn(
                    false,
                    ValueGetter.alias(
                            alias,
                            this == args.caller ? targetGetters : sourceGetters
                    ),
                    deletedIds,
                    builder
            );
            return;
        }

        IdPairs retainedIdPairs = args.retainedIdPairs;
        if (retainedIdPairs.entries().size() == 1) {
            Tuple2<Object, Collection<Object>> tuple = retainedIdPairs.entries().iterator().next();
            ComparisonPredicates.renderEq(
                    false,
                    ValueGetter.alias(alias, sourceGetters),
                    tuple.get_1(),
                    builder
            );
            if (!tuple.get_2().isEmpty()) {
                builder.separator();
                ComparisonPredicates.renderIn(
                        true,
                        ValueGetter.alias(alias, targetGetters),
                        tuple.get_2(),
                        builder
                );
            }
            return;
        }
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                ValueGetter.alias(alias, sourceGetters),
                ValueGetter.alias(alias, targetGetters),
                retainedIdPairs
        );
    }

    private boolean isJoinAllowed(Collection<Object> deletedIds, ChildTableOperator caller) {
        if (parent == null || this == caller) {
            return false;
        }
        if (deletedIds != null) {
            return parent != caller;
        }
        return true;
    }

    private void addJoinPredicates(
            AbstractSqlBuilder<?> builder,
            int depth
    ) {
        List<ValueGetter> sourceGetters = this.sourceGetters;
        List<ValueGetter> parentGetters = ValueGetter.valueGetters(
                builder.sqlClient(),
                parent.ctx.path.getType().getIdProp()
        );
        int size = sourceGetters.size();
        builder.enter(size == 1 ? AbstractSqlBuilder.ScopeType.NULL : AbstractSqlBuilder.ScopeType.AND);
        for (int i = 0; i < size; i++) {
            builder.separator()
                    .sql(alias(depth - 1))
                    .sql(".")
                    .sql(sourceGetters.get(i))
                    .sql(" = ")
                    .sql(alias(depth))
                    .sql(".")
                    .sql(parentGetters.get(i));
        }
        builder.leave();
    }

    private List<Tuple2<Object, Object>> findDisconnectingTuples(DisconnectionArgs args) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, query.getTable(), args);
        return query.select(
                query.getTableImplementor().getAssociatedId(ctx.backProp),
                query.getTableImplementor().getId()
        ).execute(con);
    }

    private List<Object> findDisconnectingIds(DisconnectionArgs  args) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, query.getTable(), args);
        return query.select(
                query.getTableImplementor().getId()
        ).execute(con);
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findDisconnectingObjects(DisconnectionArgs  args) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, query.getTable(), args);
        return query.select(
                (Selection<ImmutableSpi>)query.getTable()
        ).execute(con);
    }

    private void addDisconnectingConditions(
            AbstractMutableQueryImpl query,
            Table<?> table,
            DisconnectionArgs args
    ) {
        if (this != args.caller) {
            Table<?> parentTable = table.join(ctx.backProp);
            parent.addDisconnectingConditions(query, parentTable, args);
            return;
        }

        Collection<Object> deletedIds = args.deletedIds;
        if (deletedIds != null) {
            if (!deletedIds.isEmpty()) {
                query.where(table.getId().in(deletedIds));
            }
            return;
        }
        IdPairs retainedIdPairs = args.retainedIdPairs;
        if (retainedIdPairs.entries().size() == 1) {
            query.where(
                    table.getAssociatedId(ctx.backProp).in(
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
                table.getAssociatedId(ctx.backProp).in(
                        Tuple2.projection1(retainedIdPairs.entries())
                )
        );
        if (!retainedIdPairs.tuples().isEmpty()) {
            query.where(
                    Expression.tuple(
                            table.getAssociatedId(ctx.backProp),
                            table.getId()
                    ).notIn(retainedIdPairs.tuples())
            );
        }
    }

    private List<ChildTableOperator> subOperators() {
        List<ChildTableOperator> subOperators = null;
        if (ctx.path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backProp : sqlClient.getEntityManager().getAllBackProps(ctx.path.getType())) {
                if (backProp.isColumnDefinition() && disconnectingType != DisconnectingType.NONE) {
                    if (subOperators == null) {
                        subOperators = new ArrayList<>();
                    }
                    subOperators.add(new ChildTableOperator(this, backProp));
                }
            }
        }
        if (subOperators == null) {
            return Collections.emptyList();
        }
        return subOperators;
    }

    private List<MiddleTableOperator> middleTableOperators() {
        List<MiddleTableOperator> middleTableOperators = null;
        for (ImmutableProp prop : ctx.path.getType().getProps().values()) {
            if (prop.isMiddleTableDefinition()) {
                if (middleTableOperators == null) {
                    middleTableOperators = new ArrayList<>();
                }
                MiddleTableOperator middleTableOperator = MiddleTableOperator.propOf(this, prop);
                if (!middleTableOperator.middleTable.isReadonly()) {
                    middleTableOperators.add(middleTableOperator);
                }
            }
        }
        if (ctx.path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backProp : sqlClient.getEntityManager().getAllBackProps(ctx.path.getType())) {
                if (backProp.isMiddleTableDefinition()) {
                    if (middleTableOperators == null) {
                        middleTableOperators = new ArrayList<>();
                    }
                    MiddleTableOperator middleTableOperator = MiddleTableOperator.backPropOf(this, backProp);
                    if (!middleTableOperator.middleTable.isReadonly()) {
                        middleTableOperators.add(middleTableOperator);
                    }
                }
            }
        }
        if (middleTableOperators == null) {
            return Collections.emptyList();
        }
        return middleTableOperators;
    }

    private int currentDepth(DisconnectionArgs args) {
        return this == args.caller ? 0 : 1;
    }

    private static String alias(int depth) {
        if (depth == 0) {
            return null;
        }
        return "tb_" + depth + '_';
    }

    @Override
    public String toString() {
        return "ChildTableOperator(" + ctx.path + ")";
    }
}
