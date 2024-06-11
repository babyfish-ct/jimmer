package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;

class MiddleTableOperator {

    private final SaveContext ctx;

    private final MiddleTable middleTable;

    private final ColumnDefinition sourceColumnDefinition;

    private final ColumnDefinition targetColumnDefinition;

    private final List<ImmutableProp>[] sourcePaths;

    private final List<ImmutableProp>[] targetPaths;

    MiddleTableOperator(SaveContext ctx) {
        ImmutableProp prop = ctx.path.getProp();
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        MiddleTable mt;
        if (prop.getMappedBy() != null) {
            mt = prop.getMappedBy().getStorage(strategy);
            mt = mt.getInverse();
        } else {
            mt = prop.getStorage(strategy);
        }
        this.ctx = ctx;
        this.middleTable = mt;
        this.sourceColumnDefinition = mt.getColumnDefinition();
        this.targetColumnDefinition = mt.getTargetColumnDefinition();
        if (prop.getDeclaringType().getIdProp().isEmbedded(EmbeddedLevel.SCALAR)) {
            EmbeddedColumns embeddedColumns = prop.getDeclaringType().getIdProp().getStorage(strategy);
            List<ImmutableProp>[] arr = new List[sourceColumnDefinition.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = embeddedColumns.path(((MultipleJoinColumns)sourceColumnDefinition).referencedName(i));
            }
            sourcePaths = arr;
        } else {
            sourcePaths = null;
        }
        if (prop.getTargetType().getIdProp().isEmbedded(EmbeddedLevel.SCALAR)) {
            EmbeddedColumns embeddedColumns = prop.getTargetType().getIdProp().getStorage(strategy);
            List<ImmutableProp>[] arr = new List[targetColumnDefinition.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = embeddedColumns.path(((MultipleJoinColumns)targetColumnDefinition).referencedName(i));
            }
            targetPaths = arr;
        } else {
            targetPaths = null;
        }
    }

    public int disconnectExcept(Collection<Tuple2<Object, Object>> tuples) {
        JSqlClientImplementor sqlClient = ctx.options.getSqlClient();
        AstContext astContext = new AstContext(sqlClient);
        SqlBuilder builder = new SqlBuilder(astContext);
        builder.sql("delete from ").sql(middleTable.getTableName());
        builder.enter(SqlBuilder.ScopeType.WHERE);
        addPredicate(builder, true, tuples);
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return sqlClient
                .getExecutor()
                .execute(
                        new Executor.Args<>(
                                sqlClient,
                                ctx.con,
                                sqlResult.get_1(),
                                sqlResult.get_2(),
                                sqlResult.get_3(),
                                ExecutionPurpose.MUTATE,
                                null,
                                PreparedStatement::executeUpdate
                        )
                );
    }

    private void addPredicate(SqlBuilder builder, boolean negative, Collection<Tuple2<Object, Object>> tuples) {
        builder.separator();
        if (ctx.options.getSqlClient().getDialect().isTupleSupported()) {
            addInList(builder, negative, tuples);
        } else {
            addExpandedList(builder, negative, tuples);
        }
    }

    private void addInList(SqlBuilder builder, boolean negative, Collection<Tuple2<Object, Object>> tuples) {
        InList<Tuple2<Object, Object>> inList = new InList<>(
                tuples,
                ctx.options.getSqlClient().isInListPaddingEnabled(),
                ctx.options.getSqlClient().getDialect().getMaxInListSize()
        );
        builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
        for (Iterable<Tuple2<Object, Object>> iterable : inList) {
            builder.separator();
            addSingleInList(builder, negative, iterable);
        }
        builder.leave();
    }

    private void addSingleInList(SqlBuilder builder, boolean negative, Iterable<Tuple2<Object, Object>> tuples) {
        builder.enter(SqlBuilder.ScopeType.TUPLE);
        for (int i = 0; i < sourceColumnDefinition.size(); i++) {
            builder.separator();
            builder.sql(sourceColumnDefinition.name(i));
        }
        for (int i = 0; i < targetColumnDefinition.size(); i++) {
            builder.separator();
            builder.sql(targetColumnDefinition.name(i));
        }
        builder.leave();
        builder.sql(negative ? " not in " : " in ");
        builder.enter(SqlBuilder.ScopeType.LIST);
        for (Tuple2<Object, Object> tuple : tuples) {
            builder.separator().enter(SqlBuilder.ScopeType.TUPLE);
            for (int i = 0; i < sourceColumnDefinition.size(); i++) {
                builder.separator();
                if (sourcePaths != null) {
                    builder.variable(pathValue(tuple.get_1(), sourcePaths[i]));
                } else {
                    builder.variable(tuple.get_1());
                }
            }
            for (int i = 0; i < targetColumnDefinition.size(); i++) {
                builder.separator();
                if (targetPaths != null) {
                    builder.variable(pathValue(tuple.get_2(), targetPaths[i]));
                } else {
                    builder.variable(tuple.get_2());
                }
            }
            builder.leave();
        }
        builder.leave();
    }

    private void addExpandedList(SqlBuilder builder, boolean negative, Iterable<Tuple2<Object, Object>> tuples) {
        builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
        for (Tuple2<Object, Object> tuple : tuples) {
            builder.separator();
            if (negative) {
                builder.enter(SqlBuilder.ScopeType.SUB_QUERY).enter(SqlBuilder.ScopeType.OR);
                addExpandedItem(builder, negative, tuple);
                builder.leave().leave();
            } else {
                builder.enter(SqlBuilder.ScopeType.AND);
                addExpandedItem(builder, negative, tuple);
                builder.leave();
            }
        }
        builder.leave();
    }

    private void addExpandedItem(SqlBuilder builder, boolean negative, Tuple2<Object, Object> tuple) {
        for (int i = 0; i < sourceColumnDefinition.size(); i++) {
            builder.separator();
            builder.sql(sourceColumnDefinition.name(i));
            builder.sql(negative ? " <> " : " = ");
            if (sourcePaths != null) {
                builder.variable(pathValue(tuple.get_1(), sourcePaths[i]));
            } else {
                builder.variable(tuple.get_1());
            }
        }
        for (int i = 0; i < targetColumnDefinition.size(); i++) {
            builder.separator();
            builder.sql(targetColumnDefinition.name(i));
            builder.sql(negative ? " <> " : " = ");
            if (targetPaths != null) {
                builder.variable(pathValue(tuple.get_2(), targetPaths[i]));
            } else {
                builder.variable(tuple.get_2());
            }
        }
    }

    private void addLogicalDeletedPredicate(SqlBuilder builder) {
        LogicalDeletedInfo logicalDeletedInfo = middleTable.getLogicalDeletedInfo();
        if (logicalDeletedInfo == null) {
            return;
        }
        builder.separator();
        LogicalDeletedInfo.Action action = logicalDeletedInfo.getAction();
        if (action instanceof LogicalDeletedInfo.Action.Eq) {
            LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" = ").variable(eq.getValue());
        } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
            LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" <> ").variable(ne.getValue());
        } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" is null");
        } else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" is not null");
        }
    }

    private void addFilterPredicate(SqlBuilder builder) {
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();
        if (filterInfo == null) {
            return;
        }
        builder.separator().sql(filterInfo.getColumnName());
        if (filterInfo.getValues().size() == 1) {
            builder.sql(" = ").variable(filterInfo.getValues().get(0));
        } else {
            builder.sql(" in ").enter(SqlBuilder.ScopeType.LIST);
            for (Object value : filterInfo.getValues()) {
                builder.separator().variable(value);
            }
            builder.leave();
        }
    }

    private static Object pathValue(Object id, List<ImmutableProp> props) {
        for (ImmutableProp prop : props) {
            id = ((ImmutableSpi)id).__get(prop.getId());
            if (id == null) {
                return null;
            }
        }
        return id;
    }
}
