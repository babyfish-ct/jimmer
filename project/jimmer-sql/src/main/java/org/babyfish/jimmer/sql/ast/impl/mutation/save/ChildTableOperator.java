package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.mutation.MutableUpdateImpl;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.PreparedStatement;
import java.util.*;

class ChildTableOperator extends AbstractOperator {

    private final SaveContext ctx;

    private final String tableName;

    private final List<ValueGetter> sourceGetters;

    private final List<ValueGetter> targetGetters;

    private final List<ValueGetter> getters;

    private final boolean hasTargetFilter;

    ChildTableOperator(SaveContext ctx) {
        super(ctx.options.getSqlClient(), ctx.con);
        this.ctx = ctx;
        this.tableName = ctx.path.getType().getTableName(sqlClient.getMetadataStrategy());
        this.sourceGetters = ValueGetter.valueGetters(sqlClient, ctx.backReferenceProp);
        this.targetGetters = ValueGetter.valueGetters(sqlClient, ctx.path.getType().getIdProp());
        this.getters = ValueGetter.tupleGetters(sourceGetters, targetGetters);
        this.hasTargetFilter = sqlClient.getFilters().getTargetFilter(ctx.path.getProp()) != null;
    }

    List<Object> findDisconnectingTargetIds(IdPairs idPairs) {
        MutableRootQueryImpl<Table<?>> query =
                new MutableRootQueryImpl<>(
                        sqlClient,
                        ctx.path.getType(),
                        ExecutionPurpose.MUTATE,
                        FilterLevel.DEFAULT
                );
        TableImplementor<?> table = query.getTableImplementor();
        return null;
    }

    int disconnectExcept(IdPairs idPairs) {
        if (idPairs.entries().size() < 2) {
            Tuple2<Object, Collection<Object>> idTuple = idPairs.entries().iterator().next();
            return disconnectExceptBySimpleInPredicate(idTuple.get_1(), idTuple.get_2());
        }
        if (targetGetters.size() == 1 && sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
            return disconnectExceptByBatch(idPairs);
        }
        return disconnectExceptByComplexInPredicate(idPairs);
    }

    @SuppressWarnings("unchecked")
    private int disconnectExceptByBatch(IdPairs idPairs) {
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator()
                    .sql(sourceGetter)
                    .sql(" = ")
                    .variable(row -> {
                        Tuple2<Object, Collection<Object>> idTuple = (Tuple2<Object, Collection<Object>>) row;
                        return sourceGetter.get(idTuple.get_1());
                    });
        }
        ValueGetter targetGetter = targetGetters.get(0);
        builder.separator()
                .sql("not ")
                .enter(AbstractSqlBuilder.ScopeType.SUB_QUERY)
                .sql(targetGetter)
                .sql(" = any(")
                .variable(row -> {
                    Tuple2<Object, Collection<Object>> idTuple = (Tuple2<Object, Collection<Object>>) row;
                    Set<Object> values = new LinkedHashSet<>();
                    for (Object value : idTuple.get_2()) {
                        values.add(targetGetter.get(value));
                    }
                    return new TypedList<>(targetGetter.metadata().getSqlTypeName(), values.toArray());
                })
                .sql(")")
                .leave();
        return execute(builder, idPairs.entries());
    }

    private int disconnectExceptBySimpleInPredicate(Object sourceId, Collection<?> targetIds) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("update ")
                .sql(tableName)
                .enter(AbstractSqlBuilder.ScopeType.SET);
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator()
                    .sql(sourceGetter)
                    .sql(" = null");
        }
        builder.leave();
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        ComparisonPredicates.renderEq(
                false,
                sourceGetters,
                sourceId,
                builder
        );
        if (!targetIds.isEmpty()) {
            builder.separator();
            ComparisonPredicates.renderIn(
                    true,
                    targetGetters,
                    targetIds,
                    builder
            );
        }
        builder.leave();
        return execute(builder);
    }

    private int disconnectExceptByComplexInPredicate(IdPairs idPairs) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("update ")
                .sql(tableName)
                .enter(AbstractSqlBuilder.ScopeType.SET);
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator()
                    .sql(sourceGetter)
                    .sql(" = null");
        }
        builder.leave();
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        ComparisonPredicates.renderIn(
                false,
                sourceGetters,
                Tuple2.projection1(idPairs.entries()),
                builder
        );
        if (!idPairs.tuples().isEmpty()) {
            builder.separator();
            ComparisonPredicates.renderIn(
                    true,
                    targetGetters,
                    idPairs.tuples(),
                    builder
            );
        }
        builder.leave();
        return execute(builder);
    }

    private void addQueryConditions(MutableRootQueryImpl<?> query, IdPairs idPairs) {
        TableImplementor<?> table = query.getTableImplementor();
        for (ValueGetter sourceGetter : sourceGetters) {
            query.where(
                    table.getAssociatedId(ctx.backReferenceProp).in(
                            Tuple2.projection1(idPairs.entries())
                    )
            );
        }
        if (!idPairs.tuples().isEmpty()) {
            query.where(
                    Expression.tuple(
                            table.getAssociatedId(ctx.backReferenceProp),
                            table.getId()
                    ).notIn(idPairs.tuples())
            );
        }
    }
}
