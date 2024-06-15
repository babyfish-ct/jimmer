package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinTable;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

class MiddleTableOperator {

    private static final Object[] EMPTY_ARR = new Object[0];

    private static final KeyUniqueConstraint DEFAULT_KEY_UNIQUE_CONSTRAINT =
            AnnotationHolder.class.getAnnotation(KeyUniqueConstraint.class);

    private final ImmutableProp prop;

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final MutationTrigger trigger;

    private final MiddleTable middleTable;

    private final KeyUniqueConstraint keyUniqueConstraint;

    private final ColumnDefinition sourceColumnDefinition;

    private final ColumnDefinition targetColumnDefinition;

    private final List<ImmutableProp>[] sourcePaths;

    private final List<ImmutableProp>[] targetPaths;

    @SuppressWarnings("unchecked")
    MiddleTableOperator(SaveContext ctx) {
        ImmutableProp prop = ctx.path.getProp();
        MetadataStrategy strategy = ctx.options.getSqlClient().getMetadataStrategy();
        MiddleTable mt;
        KeyUniqueConstraint kuc = DEFAULT_KEY_UNIQUE_CONSTRAINT;
        if (prop.getMappedBy() != null) {
            mt = prop.getMappedBy().getStorage(strategy);
            mt = mt.getInverse();
            JoinTable jt = prop.getMappedBy().getAnnotation(JoinTable.class);
            if (jt != null) {
                kuc = jt.keyUniqueConstraint();
            }
        } else {
            mt = prop.getStorage(strategy);
            JoinTable jt = prop.getAnnotation(JoinTable.class);
            if (jt != null) {
                kuc = jt.keyUniqueConstraint();
            }
        }
        this.prop = prop;
        this.sqlClient = ctx.options.getSqlClient();
        this.con = ctx.con;
        this.trigger = ctx.trigger;
        this.middleTable = mt;
        this.keyUniqueConstraint = kuc;
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

    public int append(Collection<Tuple2<Object, Object>> idTuples) {
        int rowCount = connect(idTuples);
        MutationTrigger trigger = this.trigger;
        if (trigger != null) {
            for (Tuple2<Object, Object> idTuple : idTuples) {
                trigger.insertMiddleTable(prop, idTuple.get_1(), idTuple.get_2());
            }
        }
        return rowCount;
    }

    public int merge(Collection<Tuple2<Object, Object>> idTuples) {
        if (keyUniqueConstraint.noMoreUniqueConstraints() ||
                sqlClient.getDialect().isUpsertWithMultipleUniqueConstraintSupported()) {
            int[] rowCounts = connectIfNecessary(idTuples);
            int sumRowCount = 0;
            int index = 0;
            MutationTrigger trigger = this.trigger;
            for (Tuple2<Object, Object> idTuple : idTuples) {
                if (rowCounts[index++] != 0) {
                    sumRowCount++;
                    if (trigger != null) {
                        trigger.insertMiddleTable(prop, idTuple.get_1(), idTuple.get_2());
                    }
                }
            }
            return sumRowCount;
        }
        List<Object> sourceIds = new ArrayList<>();
        for (Tuple2<Object, Object> idTuple : idTuples) {
            sourceIds.add(idTuple.get_1());
        }
        Set<Tuple2<Object, Object>> existingIdTuples = find(sourceIds);
        List<Tuple2<Object, Object>> insertingIdTuples =
                new ArrayList<>(idTuples.size() - existingIdTuples.size());
        for (Tuple2<Object, Object> idTuple : idTuples) {
            if (!existingIdTuples.contains(idTuple)) {
                insertingIdTuples.add(idTuple);
            }
        }
        return connect(insertingIdTuples);
    }

    public int replace(Collection<Tuple2<Object, Object>> idTuples) {
        MutationTrigger trigger = this.trigger;
        if (trigger == null) {
            int[] rowCounts = connectIfNecessary(idTuples);
            int sumRowCount = disconnectExcept(idTuples);
            for (int rowCount : rowCounts) {
                if (rowCount != 0) {
                    sumRowCount += rowCount;
                }
            }
            return sumRowCount;
        }
        List<Object> sourceIds = new ArrayList<>();
        for (Tuple2<Object, Object> idTuple : idTuples) {
            sourceIds.add(idTuple.get_1());
        }
        Set<Tuple2<Object, Object>> deletingIdTuples = find(sourceIds);
        List<Tuple2<Object, Object>> insertingIdTuples =
                new ArrayList<>(idTuples.size() - deletingIdTuples.size());
        Iterator<Tuple2<Object, Object>> itr = idTuples.iterator();
        while (itr.hasNext()) {
            Tuple2<Object, Object> idTuple = itr.next();
            if (deletingIdTuples.contains(idTuple)) {
                itr.remove();
            } else {
                insertingIdTuples.add(idTuple);
            }
        }
        return connect(insertingIdTuples) + disconnect(deletingIdTuples);
    }

    @SuppressWarnings("unchecked")
    Set<Tuple2<Object, Object>> find(Collection<Object> ids) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.sql("select ").definition(sourceColumnDefinition)
                .sql(", ").definition(targetColumnDefinition)
                .sql(" from ").sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        if (sourceColumnDefinition.size() == 1) {
            if (sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
                String sourceIdSqlType = prop.getDeclaringType().getIdProp()
                        .<SingleColumn>getStorage(sqlClient.getMetadataStrategy())
                        .getSqlType();
                builder.separator()
                        .definition(sourceColumnDefinition)
                        .sql(" = any(")
                        .variable(new TypedList<Object>(sourceIdSqlType, ids.toArray(EMPTY_ARR)))
                        .sql(")");
            } else {
                builder.separator().enter(SqlBuilder.ScopeType.OR);
                for (Iterable<Object> iterable : new InList<>(
                        ids,
                        sqlClient.isInListPaddingEnabled(),
                        sqlClient.getDialect().getMaxInListSize()
                )) {
                    builder.separator()
                            .definition(sourceColumnDefinition)
                            .sql(" in ")
                            .enter(SqlBuilder.ScopeType.LIST);
                    for (Object id : iterable) {
                        builder.separator().variable(id);
                    }
                    builder.leave();
                }
                builder.leave();
            }
        } else {
            builder.separator().enter(SqlBuilder.ScopeType.OR);
            for (Iterable<Object> iterable : new InList<>(
                    ids,
                    sqlClient.isInListPaddingEnabled(),
                    sqlClient.getDialect().getMaxInListSize()
            )) {
                builder.separator()
                        .sql("(")
                        .definition(sourceColumnDefinition)
                        .sql(") in ")
                        .enter(SqlBuilder.ScopeType.LIST);
                for (Object id : iterable) {
                    builder.separator().enter(SqlBuilder.ScopeType.TUPLE);
                    for (List<ImmutableProp> path : sourcePaths) {
                        builder.separator().variable(pathValue(id, path));
                    }
                    builder.leave();
                }
                builder.leave();
            }
            builder.leave();
        }
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Reader<Object> sourceIdReader = (Reader<Object>) sqlClient.getReader(prop.getDeclaringType().getIdProp());
        Reader<Object> targetIdReader = (Reader<Object>) sqlClient.getReader(prop.getTargetType().getIdProp());
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.QUERY,
                        null,
                        stmt -> {
                            Reader.Context ctx = new Reader.Context(null, sqlClient);
                            Set<Tuple2<Object, Object>> idTuples = new LinkedHashSet<>();
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    ctx.resetCol();
                                    Object sourceId = sourceIdReader.read(rs, ctx);
                                    Object targetId = targetIdReader.read(rs, ctx);
                                    idTuples.add(new Tuple2<>(sourceId, targetId));
                                }
                            }
                            return idTuples;
                        }
                )
        );
    }

    int connect(Collection<Tuple2<Object, Object>> tuples) {
        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        builder.sql("insert into ").sql(middleTable.getTableName()).enter(TemplateBuilder.ScopeType.TUPLE);
        appendColumns(builder);
        builder.leave();
        builder.sql(" values").enter(TemplateBuilder.ScopeType.TUPLE);
        appendValues(builder);
        builder.leave();
        return execute(builder, tuples);
    }

    int[] connectIfNecessary(Collection<Tuple2<Object, Object>> tuples) {
        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        sqlClient.getDialect().upsert(new UpsertContextImpl(builder));
        return executeImpl(builder, tuples);
    }

    @SuppressWarnings("unchecked")
    int disconnect(Collection<Tuple2<Object, Object>> tuples) {
        TemplateBuilder builder = new TemplateBuilder(sqlClient);
        builder.sql("delete from ").sql(middleTable.getTableName()).enter(TemplateBuilder.ScopeType.WHERE);
        for (int i = 0; i < sourceColumnDefinition.size(); i++) {
            List<ImmutableProp> props = sourcePaths != null ? sourcePaths[i] : null;
            builder.separator().sql(sourceColumnDefinition.name(i)).sql(" = ").variable(row -> {
                Tuple2<Object, Object> tuple = (Tuple2<Object, Object>) row;
                return pathValue(tuple.get_1(), props);
            });
        }
        for (int i = 0; i < targetColumnDefinition.size(); i++) {
            List<ImmutableProp> props = targetPaths != null ? targetPaths[i] : null;
            builder.separator().sql(targetColumnDefinition.name(i)).sql(" = ").variable(row -> {
                Tuple2<Object, Object> tuple = (Tuple2<Object, Object>) row;
                return pathValue(tuple.get_2(), props);
            });
        }
        builder.leave();
        return execute(builder, tuples);
    }

    int disconnectExcept(Collection<Tuple2<Object, Object>> tuples) {
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
                                con,
                                sqlResult.get_1(),
                                sqlResult.get_2(),
                                sqlResult.get_3(),
                                ExecutionPurpose.MUTATE,
                                null,
                                PreparedStatement::executeUpdate
                        )
                );
    }

    private int execute(TemplateBuilder builder, Collection<Tuple2<Object, Object>> tuples) {
        int[] rowCounts = executeImpl(builder, tuples);
        int sumRowCount = 0;
        for (int rowCount : rowCounts) {
            if (rowCount != 0) {
                sumRowCount++;
            }
        }
        return sumRowCount;
    }

    private int[] executeImpl(TemplateBuilder builder, Collection<Tuple2<Object, Object>> tuples) {
        Tuple2<String, TemplateBuilder.VariableMapper> sqlTuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor().executeBatch(
                        sqlClient,
                        con,
                        sqlTuple.get_1(),
                        null
                )
        ) {
            TemplateBuilder.VariableMapper mapper = sqlTuple.get_2();
            for (Tuple2<Object, Object> tuple : tuples) {
                batchContext.add(mapper.variables(tuple));
            }
            return batchContext.execute();
        }
    }

    private void addPredicate(SqlBuilder builder, boolean negative, Collection<Tuple2<Object, Object>> tuples) {
        builder.separator();
        if (sqlClient.getDialect().isTupleSupported()) {
            addInList(builder, negative, tuples);
        } else {
            addExpandedList(builder, negative, tuples);
        }
    }

    private void addInList(SqlBuilder builder, boolean negative, Collection<Tuple2<Object, Object>> tuples) {
        InList<Tuple2<Object, Object>> inList = new InList<>(
                tuples,
                sqlClient.isInListPaddingEnabled(),
                sqlClient.getDialect().getMaxInListSize()
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
        for (String columnName : sourceColumnDefinition) {
            builder.separator().sql(columnName);
        }
        for (String columnName : targetColumnDefinition) {
            builder.separator().sql(columnName);
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
        if (props != null) {
            for (ImmutableProp prop : props) {
                id = ((ImmutableSpi) id).__get(prop.getId());
                if (id == null) {
                    return null;
                }
            }
        }
        return id;
    }

    private void appendColumns(TemplateBuilder builder) {
        for (String columnName : sourceColumnDefinition) {
            builder.separator().sql(columnName);
        }
        for (String columnName : targetColumnDefinition) {
            builder.separator().sql(columnName);
        }
        if (middleTable.getLogicalDeletedInfo() != null) {
            builder.separator().sql(middleTable.getLogicalDeletedInfo().getColumnName());
        }
        if (middleTable.getFilterInfo() != null) {
            builder.separator().sql(middleTable.getFilterInfo().getColumnName());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendValues(TemplateBuilder builder) {
        for (int i = 0; i < sourceColumnDefinition.size(); i++) {
            List<ImmutableProp> props = sourcePaths != null ? sourcePaths[i] : null;
            builder.separator().variable(row -> {
                Tuple2<Object, Object> tuple = (Tuple2<Object, Object>) row;
                return pathValue(tuple.get_1(), props);
            });
        }
        for (int i = 0; i < targetColumnDefinition.size(); i++) {
            List<ImmutableProp> props = targetPaths != null ? targetPaths[i] : null;
            builder.separator().variable(row -> {
                Tuple2<Object, Object> tuple = (Tuple2<Object, Object>) row;
                return pathValue(tuple.get_2(), props);
            });
        }
        if (middleTable.getLogicalDeletedInfo() != null) {
            builder.separator().value(middleTable.getLogicalDeletedInfo().allocateInitializedValue());
        }
        if (middleTable.getFilterInfo() != null) {
            builder.separator().value(middleTable.getFilterInfo().getValues().get(0));
        }
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final TemplateBuilder builder;

        UpsertContextImpl(TemplateBuilder builder) {
            this.builder = builder;
        }

        @Override
        public boolean hasUpdatedColumns() {
            return false;
        }

        @Override
        public boolean hasOptimisticLock() {
            return false;
        }

        @Override
        public Dialect.UpsertContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpsertContext appendTableName() {
            builder.sql(middleTable.getTableName());
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns() {
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            appendColumns(builder);
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            appendColumns(builder);
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertingValues() {
            builder.enter(TemplateBuilder.ScopeType.COMMA);
            appendValues(builder);
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition() {
            return this;
        }
    }

    @KeyUniqueConstraint
    private static class AnnotationHolder {}
}
