package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
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
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;
import java.util.stream.Collectors;

class ChildTableOperator extends AbstractAssociationOperator {

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
        super(
                ctx.options.getSqlClient(),
                ctx.con,
                ctx.options.isBatchForbidden(),
                ctx.options.getExceptionTranslator()
        );
        if (ctx.backProp == null) {
            throw new IllegalArgumentException(
                    "The delete context for child table operator must have back prop"
            );
        }
        if (!ctx.backProp.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The delete context for child table operator is \"" +
                            ctx.backProp +
                            "\" which is not based on columns"
            );
        }
        DissociateAction dissociateAction =
                        ctx.options.getDissociateAction(ctx.path.getBackProp());
        DisconnectingType disconnectingType;
        switch (dissociateAction) {
            case CHECK:
                disconnectingType = DisconnectingType.CHECKING;
                break;
            case SET_NULL:
                disconnectingType = DisconnectingType.SET_NULL;
                break;
            case DELETE:
                if (ctx.isLogicalDeleted() && (
                        (ctx.parent != null && ctx.parent.isLogicalDeleted()) ||
                        !ctx.backProp.isTargetForeignKeyReal(ctx.options.getSqlClient().getMetadataStrategy())
                )) {
                    disconnectingType = DisconnectingType.LOGICAL_DELETE;
                } else {
                    disconnectingType = DisconnectingType.PHYSICAL_DELETE;
                }
                break;
            default:
                disconnectingType = DisconnectingType.NONE;
                break;
        }
        QueryReason queryReason = QueryReason.NONE;
        if (disconnectingType == DisconnectingType.CHECKING) {
            queryReason = QueryReason.CHECKING;
        } else if (ctx.trigger != null) {
            queryReason = QueryReason.TRIGGER;
        }
        int mutationSubQueryDepth = parent != null ? parent.mutationSubQueryDepth + 1 : 1;
        if (mutationSubQueryDepth > 1 && !ctx.options.getSqlClient().getDialect().isTableOfSubQueryMutable()) {
            mutationSubQueryDepth = 0;
            queryReason = QueryReason.CANNOT_MUTATE_TABLE_OF_SUB_QUERY;
        } else if (mutationSubQueryDepth > ctx.options.getMaxCommandJoinCount()) {
            mutationSubQueryDepth = 0;
            queryReason = QueryReason.TOO_DEEP;
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

    final void disconnect(Collection<Object> ids) {
        disconnect(DisconnectionArgs.delete(ids, null).withTrigger(true));
    }

    final void disconnectExcept(IdPairs.Retain idPairs) {
        disconnectExcept(idPairs, false);
    }

    final void disconnectExcept(IdPairs.Retain idPairs, boolean force) {
        disconnect(
                DisconnectionArgs.retain(idPairs, this).withTrigger(true).withForce(force)
        );
    }

    private void disconnect(DisconnectionArgs args) {
        if (args.isEmpty()) {
            return;
        }
        if (!args.force && disconnectingType == DisconnectingType.NONE) {
            return;
        }
        if (disconnectingType == DisconnectingType.NONE || disconnectingType == DisconnectingType.CHECKING) {
            List<Object> ids = findDisconnectingIds(args, 1);
            if (!ids.isEmpty()) {
                ctx.throwCannotDissociateTarget();
            }
            return;
        }
        if (ctx.trigger != null) {
            List<ImmutableSpi> rows = findDisconnectingObjects(args);
            if (rows.isEmpty()) {
                return;
            }
            if (args.deletedIds == null || args.caller != this) {
                PropId idPropId = ctx.path.getType().getIdProp().getId();;
                args = DisconnectionArgs.delete(
                        rows
                                .stream()
                                .map(row -> row.__get(idPropId))
                                .collect(Collectors.toList()),
                        this
                ).withTrigger(args.fireEvents);
            }
            if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
                Object generatedValue = LogicalDeletedValueGenerators
                        .of(ctx.path.getType().getLogicalDeletedInfo(), sqlClient)
                        .generate();
                args = args.withLogicalDeletedValue(generatedValue);
            }
            for (ImmutableSpi row : rows) {
                ImmutableProp prop;
                Object value;
                switch (disconnectingType) {
                    case LOGICAL_DELETE:
                        prop = ctx.path.getType().getLogicalDeletedInfo().getProp();
                        value = args.logicalDeletedValueRef.getValue();
                        break;
                    case SET_NULL:
                        prop = ctx.backProp;
                        value = null;
                        break;
                    default:
                        prop = null;
                        value = null;
                        break;
                }
                Deleter.fireEvent(
                        row,
                        prop,
                        value,
                        ctx.trigger
                );
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
        if (queryReason == QueryReason.TUPLE_IS_UNSUPPORTED && (
                args.retainedIdPairs == null || args.retainedIdPairs.tuples().size() <= 1)) {
            return null;
        }
        return findDisconnectingIds(args, 0);
    }

    private void disconnectImpl(DisconnectionArgs args) {
        if (args.deletedIds != null) {
            SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
            addOperationHead(builder, args, currentDepth(args));
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
        addOperationHead(builder, args, currentDepth(args));
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, args, currentDepth(args));
        builder.leave();
        int rowCount = execute(builder, args.retainedIdPairs.entries(), null);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    private void disconnectExceptByInPredicate(DisconnectionArgs args) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperationHead(builder, args, currentDepth(args));
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicates(builder, args, currentDepth(args));
        builder.leave();
        int rowCount = execute(builder);
        AffectedRows.add(ctx.affectedRowCountMap, ctx.path.getType(), rowCount);
    }

    private void addOperationHead(
            AbstractSqlBuilder<?> builder,
            DisconnectionArgs args,
            int depth
    ) {
        if (disconnectingType == DisconnectingType.PHYSICAL_DELETE) {
            builder.sql("delete from ").sql(tableName);
            addOperationHeadAlias(builder, depth, "delete");
        } else if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            assert logicalDeletedInfo != null;
            builder.sql("update ").sql(tableName);
            addOperationHeadAlias(builder, depth, "update");
            builder
                    .enter(AbstractSqlBuilder.ScopeType.SET)
                    .logicalDeleteAssignment(logicalDeletedInfo, args.logicalDeletedValueRef,null)
                    .leave();
        } else {
            builder.sql("update ")
                    .sql(tableName);
            addOperationHeadAlias(builder, depth, "update");
            builder.enter(AbstractSqlBuilder.ScopeType.SET);
            for (ValueGetter sourceGetter : sourceGetters) {
                builder.separator()
                        .sql(sourceGetter)
                        .sql(" = null");
            }
            builder.leave();
        }
    }

    private void addOperationHeadAlias(
            AbstractSqlBuilder<?> builder,
            int depth,
            String operation
    ) {
        boolean alias = depth != 0;
        if (depth == 1 && (operation.equals("delete") && !sqlClient.getDialect().isDeleteAliasSupported() || (operation.equals("update") && !sqlClient.getDialect().isUpdateAliasSupported()))) {
            alias = false;
        }

        if (alias) {
            builder.sql(" ").sql(alias(depth));
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
        } else {
            addPredicatesImpl((SqlBuilder) builder, args, depth);
        }
        if (disconnectingType != DisconnectingType.PHYSICAL_DELETE) {
            LogicalDeletedInfo logicalDeletedInfo = ctx.path.getType().getLogicalDeletedInfo();
            if (logicalDeletedInfo != null) {
                builder.sql(" and ").logicalDeleteFilter(logicalDeletedInfo, alias(depth));
            }
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
            for (ValueGetter getter : this == caller ? targetGetters : sourceGetters) {
                builder.separator();
                builder.sql(getter)
                        .sql(" = ")
                        .variable(getter);
            }
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
            ComparisonPredicates.renderCmp(
                    "=",
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
        for (int i = 0; i < size; i++) {
            final int previousDepth = depth - 1;
            String alias = alias(previousDepth);

            if (previousDepth == 1 && !sqlClient.getDialect().isDeleteAliasSupported()) {
                alias = tableName;
            }

            builder.separator()
                    .sql(alias).sql(".")
                    .sql(sourceGetters.get(i))
                    .sql(" = ")
                    .sql(alias(depth))
                    .sql(".")
                    .sql(parentGetters.get(i));
        }
    }

    private List<Object> findDisconnectingIds(DisconnectionArgs args, int limit) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.command(queryReason),
                        disconnectingType.isDelete() && !ctx.isLogicalDeleted() ?
                                FilterLevel.IGNORE_ALL :
                                FilterLevel.IGNORE_USER_FILTERS
                );
        addDisconnectingConditions(query, query.getTable(), args);
        ConfigurableRootQuery<Table<?>, Object> typedQuery = query.select(
                ((TableImplementor<?>)query.getTableLikeImplementor()).getId()
        );
        if (limit > 0) {
            typedQuery = typedQuery.limit(limit);
        }
        List<Object> ids = typedQuery.execute(con);
        for (Object id : ids) {
            ctx.addDisconnectedId(id);
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private List<ImmutableSpi> findDisconnectingObjects(DisconnectionArgs  args) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.command(queryReason),
                        disconnectingType.isDelete() && !ctx.isLogicalDeleted() ?
                                FilterLevel.IGNORE_ALL :
                                FilterLevel.IGNORE_USER_FILTERS
                );
        addDisconnectingConditions(query, query.getTable(), args);
        List<ImmutableSpi> rows = query.select(
                (Selection<ImmutableSpi>) query.getTable()
        ).execute(con);
        PropId idPropId = ctx.path.getType().getIdProp().getId();
        for (ImmutableSpi row : rows) {
            Object id = row.__get(idPropId);
            ctx.addDisconnectedId(id);
        }
        return rows;
    }

    private void addDisconnectingConditions(
            AbstractMutableQueryImpl query,
            Table<?> table,
            DisconnectionArgs args
    ) {
        if (this != args.caller && parent != null) {
            Table<?> parentTable = table.join(ctx.backProp);
            parent.addDisconnectingConditions(query, parentTable, args);
            return;
        }

        Collection<Object> deletedIds = args.deletedIds;
        if (deletedIds != null) {
            if (!deletedIds.isEmpty()) {
                if (this == args.caller) {
                    query.where(table.getId().in(deletedIds));
                } else {
                    query.where(table.getAssociatedId(ctx.backProp).in(deletedIds));
                }
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
        return createSubOperators(
                ctx.options.getSqlClient(),
                ctx.path, disconnectingType,
                backProp -> new ChildTableOperator(this, backProp)
        );
    }

    private List<MiddleTableOperator> middleTableOperators() {
        return createMiddleTableOperators(
                ctx.options.getSqlClient(),
                ctx.path,
                disconnectingType,
                prop -> MiddleTableOperator.propOf(this, prop),
                backProp -> MiddleTableOperator.backPropOf(this, backProp)
        );
    }

    private int currentDepth(DisconnectionArgs args) {
        return isJoinAllowed(args.deletedIds, args.caller) ? 1 : 0;
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
