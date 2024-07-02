package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
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

    private final QueryReason queryReason;

    private final DisconnectingType disconnectingType;

    private final ChildTableOperator parent;

    private final String tableName;

    private final List<ValueGetter> sourceGetters;

    private final List<ValueGetter> targetGetters;

    private List<ChildTableOperator> subOperators;

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
        this.ctx = ctx;
        this.queryReason = queryReason;
        this.disconnectingType = disconnectingType;
        this.parent = parent;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.sourceGetters = ValueGetter.valueGetters(sqlClient, ctx.backReferenceProp);
        this.targetGetters = ValueGetter.valueGetters(sqlClient, ctx.path.getType().getIdProp());
        if (ctx.path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backReferenceProp : sqlClient.getEntityManager().getAllBackProps(ctx.path.getType())) {
                if (backReferenceProp.isColumnDefinition() && disconnectingType != DisconnectingType.NONE) {
                    List<ChildTableOperator> subOperators = this.subOperators;
                    if (subOperators == null) {
                        this.subOperators = subOperators = new ArrayList<>();
                    }
                    subOperators.add(new ChildTableOperator(this, backReferenceProp));
                }
            }
        }
    }

    IdPairs findDisconnectingIdPairs(IdPairs idPairs) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        addDisconnectingConditions(query, idPairs);
        List<Tuple2<Object, Object>> tuples = query.select(
                query.getTableImplementor().getAssociatedId(ctx.backReferenceProp),
                query.getTableImplementor().getId()
        ).execute(con);
        return IdPairs.of(tuples);
    }

    private void addDisconnectingConditions(MutableRootQueryImpl<?> query, IdPairs idPairs) {
        TableImplementor<?> table = query.getTableImplementor();
        if (idPairs.entries().size() == 1) {
            query.where(
                    table.getAssociatedId(ctx.backReferenceProp).in(
                            Tuple2.projection1(idPairs.entries())
                    )
            );
            if (!idPairs.tuples().isEmpty()) {
                query.where(
                        table.getId().notIn(
                                Tuple2.projection2(idPairs.tuples())
                        )
                );
            }
            return;
        }
        query.where(
                table.getAssociatedId(ctx.backReferenceProp).in(
                        Tuple2.projection1(idPairs.entries())
                )
        );
        if (!idPairs.tuples().isEmpty()) {
            query.where(
                    Expression.tuple(
                            table.getAssociatedId(ctx.backReferenceProp),
                            table.getId()
                    ).notIn(idPairs.tuples())
            );
        }
    }

    int disconnectExcept(IdPairs idPairs) {
        if (subOperators != null) {
            for (ChildTableOperator subOperator : subOperators) {
                subOperator.disconnectExcept(idPairs);
            }
        }
        return disconnectExceptImpl(idPairs);
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
                    deletedIds,
                    retainedIdPairs
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
            parent.addPredicatesImpl(builder, deletedIds, retainedIdPairs);
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
