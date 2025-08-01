package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.GetterMetadata;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

class MiddleTableOperator extends AbstractAssociationOperator {

    private static final int[] EMPTY_ROW_COUNTS = new int[0];

    private static final int[] SINGLE_ERROR_ROW_COUNTS = new int[] {-1};

    private final MutationPath path;

    private final ExceptionTranslator<Exception> exceptionTranslator;
    
    private final MutationTrigger trigger;

    final Map<AffectedTable, Integer> affectedRowCount;

    final MiddleTable middleTable;

    private final List<ValueGetter> sourceGetters;

    private final List<ValueGetter> targetGetters;

    private final List<ValueGetter> referenceGetters;

    private final List<ValueGetter> getters;

    private final DisconnectingType disconnectingType;

    private final QueryReason queryReason;

    private final ChildTableOperator parent;

    private final String alias;

    MiddleTableOperator(SaveContext ctx, boolean isSourceLogicalDeleted) {
        this(
                ctx.options.getSqlClient(), 
                ctx.con,
                ctx.options.isBatchForbidden(),
                ctx.options.getExceptionTranslator(),
                ctx.options.getMaxCommandJoinCount(),
                ctx.path,
                ctx.trigger,
                ctx.affectedRowCountMap,
                null,
                isSourceLogicalDeleted
        );
    }

    static MiddleTableOperator propOf(ChildTableOperator parent, ImmutableProp prop) {
        return new MiddleTableOperator(parent, parent.ctx.propOf(prop));
    }

    static MiddleTableOperator backPropOf(ChildTableOperator parent, ImmutableProp backProp) {
        return new MiddleTableOperator(parent, parent.ctx.backPropOf(backProp));
    }

    private MiddleTableOperator(ChildTableOperator parent, DeleteContext ctx) {
        this(
                ctx.options.getSqlClient(),
                ctx.con,
                ctx.options.isBatchForbidden(),
                ctx.options.getExceptionTranslator(),
                ctx.options.getMaxCommandJoinCount(),
                ctx.path,
                ctx.trigger,
                ctx.affectedRowCountMap,
                parent,
                parent.disconnectingType == DisconnectingType.LOGICAL_DELETE
        );
    }

    @SuppressWarnings("unchecked")
    MiddleTableOperator(
            JSqlClientImplementor sqlClient,
            Connection con,
            boolean batchForbidden,
            ExceptionTranslator<?> exceptionTranslator,
            int maxCommandJoinCount,
            MutationPath path,
            MutationTrigger trigger,
            Map<AffectedTable, Integer> affectedRowCountMap,
            ChildTableOperator parent,
            boolean isSourceLogicalDeleted
    ) {
        super(sqlClient, con, batchForbidden, exceptionTranslator);
        ImmutableProp associationProp = path.getProp();
        boolean inverse = false;
        if (associationProp != null) {
            if (associationProp.getMappedBy() != null) {
                associationProp = associationProp.getMappedBy();
                inverse = true;
            }
        } else {
            associationProp = path.getBackProp();
            if (associationProp.getMappedBy() != null) {
                associationProp = associationProp.getMappedBy();
            } else {
                inverse = true;
            }
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        DisconnectingType disconnectingType;
        this.path = path;
        this.exceptionTranslator = (ExceptionTranslator<Exception>) exceptionTranslator;
        this.trigger = trigger;
        this.affectedRowCount = affectedRowCountMap;
        if (inverse) {
            this.middleTable = associationProp
                    .<MiddleTable>getStorage(strategy)
                    .getInverse();
            AssociationType associationType = AssociationType.of(associationProp);
            this.sourceGetters = ValueGetter.valueGetters(sqlClient, associationType.getTargetProp());
            this.targetGetters = ValueGetter.valueGetters(sqlClient, associationType.getSourceProp());
        } else {
            this.middleTable = associationProp.getStorage(strategy);
            AssociationType associationType = AssociationType.of(associationProp);
            this.sourceGetters = ValueGetter.valueGetters(sqlClient, associationType.getSourceProp());
            this.targetGetters = ValueGetter.valueGetters(sqlClient, associationType.getTargetProp());
        }
        if (middleTable.getLogicalDeletedInfo() == null) {
            disconnectingType = DisconnectingType.PHYSICAL_DELETE;
        } else if (middleTable.isDeletedWhenEndpointIsLogicallyDeleted()) {
            disconnectingType = DisconnectingType.PHYSICAL_DELETE;
        } else if (parent != null && parent.disconnectingType == DisconnectingType.PHYSICAL_DELETE) {
            disconnectingType = DisconnectingType.PHYSICAL_DELETE;
        } else if (path.getParent().getType().getLogicalDeletedInfo() == null) {
            disconnectingType = DisconnectingType.PHYSICAL_DELETE;
        } else if (!isSourceLogicalDeleted) {
            disconnectingType = DisconnectingType.PHYSICAL_DELETE;
        } else if (middleTable.isCascadeDeletedBySource() || middleTable.getColumnDefinition().isForeignKey()) {
            disconnectingType = DisconnectingType.LOGICAL_DELETE;
        } else {
            disconnectingType = DisconnectingType.LOGICAL_DELETE;
        }
        QueryReason queryReason = QueryReason.NONE;
        if (trigger != null) {
            queryReason = QueryReason.TRIGGER;
        } else if (parent != null && !sqlClient.getDialect().isTableOfSubQueryMutable()) {
            queryReason = QueryReason.CANNOT_MUTATE_TABLE_OF_SUB_QUERY;
        } else if (parent != null && parent.mutationSubQueryDepth >= maxCommandJoinCount) {
            queryReason = QueryReason.TOO_DEEP;
        } else if (!sqlClient.getDialect().isUpsertSupported()) {
            queryReason = QueryReason.UPSERT_NOT_SUPPORTED;
        } else if (disconnectingType.isDelete()) {
            if (sourceGetters.size() > 1 && !sqlClient.getDialect().isTupleSupported()) {
                queryReason = QueryReason.TUPLE_IS_UNSUPPORTED;
            }
        }
        this.disconnectingType = disconnectingType;
        this.queryReason = queryReason;
        this.referenceGetters = ValueGetter.tupleGetters(sourceGetters, targetGetters);
        List<ValueGetter> getters = new ArrayList<>(referenceGetters);
        if (middleTable.getLogicalDeletedInfo() != null) {
            getters.add(new DeletedGetter());
        }
        if (middleTable.getFilterInfo() != null) {
            getters.add(new FilterGetter());
        }
        this.getters = getters;
        this.parent = parent;
        this.alias = parent != null ? "tb_1_" : null;
    }

    public void append(IdPairs idPairs) {
        connect(idPairs);
        MutationTrigger trigger = this.trigger;
        if (trigger != null) {
            for (Tuple2<Object, Object> idTuple : idPairs.tuples()) {
                fireInsert(idTuple.get_1(), idTuple.get_2());
            }
        }
    }

    public final void merge(IdPairs idPairs) {
        if (queryReason == QueryReason.NONE) {
            int[] rowCounts = connectIfNecessary(idPairs);
            int index = 0;
            MutationTrigger trigger = this.trigger;
            for (Tuple2<Object, Object> idTuple : idPairs.tuples()) {
                if (rowCounts[index++] != 0) {
                    if (trigger != null) {
                        fireInsert(idTuple.get_1(), idTuple.get_2());
                    }
                }
            }
            return;
        }
        Set<Tuple2<Object, Object>> existingIdTuples = findByTuples(
                idPairs.tuples(),
                queryReason
        );
        List<Tuple2<Object, Object>> insertingIdTuples =
                new ArrayList<>(idPairs.tuples().size() - existingIdTuples.size());
        for (Tuple2<Object, Object> idTuple : idPairs.tuples()) {
            if (!existingIdTuples.contains(idTuple)) {
                insertingIdTuples.add(idTuple);
            }
        }
        append(IdPairs.of(insertingIdTuples));
    }

    public final void delete(IdPairs idPairs) {
        MutationTrigger trigger = this.trigger;
        if (trigger == null) {
            disconnect(idPairs);
            return;
        }
        Collection<Tuple2<Object, Object>> idTuples = idPairs.tuples();
        idTuples = findByTuples(idTuples, null);
        for (Tuple2<Object, Object> idTuple : idTuples) {
            fireDelete(idTuple.get_1(), idTuple.get_2());
        }
        disconnect(idPairs);
    }

    public final void replace(IdPairs.Retain idPairs) {
        MutationTrigger trigger = this.trigger;
        if (trigger == null && isUpsertUsed()) {
            disconnectExcept(idPairs);
            connectIfNecessary(idPairs);
            return;
        }
        Collection<Tuple2<Object, Object>> idTuples = idPairs.tuples();
        if (!(idTuples instanceof Set<?>)) {
            idTuples = new LinkedHashSet<>(idTuples);
        }
        Set<Object> sourceIds = new LinkedHashSet<>();
        for (Tuple2<Object, Collection<Object>> tuple : idPairs.entries()) {
            sourceIds.add(tuple.get_1());
        }
        Set<Tuple2<Object, Object>> existingIdTuples = find(sourceIds);
        List<Tuple2<Object, Object>> insertingIdTuples = new ArrayList<>();
        List<Tuple2<Object, Object>> deletingIdTuples =
                new ArrayList<>();
        for (Tuple2<Object, Object> idTuple : idTuples) {
            if (!existingIdTuples.contains(idTuple)) {
                insertingIdTuples.add(idTuple);
            }
        }
        for (Tuple2<Object, Object> existingIdTuple : existingIdTuples) {
            if (!idTuples.contains(existingIdTuple)) {
                deletingIdTuples.add(existingIdTuple);
            }
        }
        disconnect(IdPairs.of(deletingIdTuples));
        connect(IdPairs.of(insertingIdTuples));
        if (trigger != null) {
            for (Tuple2<Object, Object> idTuple : insertingIdTuples) {
                fireInsert(idTuple.get_1(), idTuple.get_2());
            }
            for (Tuple2<Object, Object> idTuple : deletingIdTuples) {
                fireDelete(idTuple.get_1(), idTuple.get_2());
            }
        }
    }

    final Set<Tuple2<Object, Object>> find(Collection<Object> ids) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
        if (ids.size() > 1) {
            for (ValueGetter getter : sourceGetters) {
                builder.separator().sql(getter);
            }
        }
        for (ValueGetter getter : targetGetters) {
            builder.separator().sql(getter);
        }
        builder.leave();
        builder
                .sql(" from ").sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        ComparisonPredicates.renderIn(
                false,
                sourceGetters,
                ids,
                builder
        );
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        return find(
                ids.size() == 1 ? ids.iterator().next() : null,
                builder,
                null
        );
    }

    final Set<Tuple2<Object, Object>> findByTuples(
            Collection<Tuple2<Object, Object>> idTuples,
            @Nullable QueryReason optionalQueryReason
    ) {
        if (idTuples.isEmpty()) {
            return Collections.emptySet();
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
        if (idTuples.size() > 1) {
            for (ValueGetter getter : sourceGetters) {
                builder.separator().sql(getter);
            }
        }
        for (ValueGetter getter : targetGetters) {
            builder.separator().sql(getter);
        }
        builder.leave();
        builder
                .sql(" from ").sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        List<TupleImplementor> rows = new ArrayList<>(idTuples.size());
        for (Tuple2<Object, Object> tuple : idTuples) {
            Object[] arr = new Object[getters.size()];
            int index = 0;
            Object a = tuple.get_1();
            Object b = tuple.get_2();
            if (a instanceof TupleImplementor) {
                index += ((TupleImplementor) a).copyTo(arr, 0);
            } else {
                arr[index++] = a;
            }
            if (b instanceof TupleImplementor) {
                ((TupleImplementor) b).copyTo(arr, 0);
            } else {
                arr[index] = b;
            }
            rows.add(Tuples.valueOf(arr));
        }
        ComparisonPredicates.renderIn(
                false,
                referenceGetters,
                rows,
                builder
        );
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        return find(
                idTuples.size() == 1 ? idTuples.iterator().next().get_1() : null,
                builder,
                optionalQueryReason
        );
    }

    private Set<Tuple2<Object, Object>> find(DisconnectionArgs args) {
        if (args.deletedIds != null && args.caller == parent) {
            return find(args.deletedIds);
        }
        if (args.isEmpty()) {
            return Collections.emptySet();
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder.enter(AbstractSqlBuilder.ScopeType.SELECT);
        for (ValueGetter getter : referenceGetters) {
            builder.separator().sql("tb_1_.").sql(getter);
        }
        builder.leave();
        builder.sql(" from ").sql(middleTable.getTableName()).sql(" tb_1_");
        builder.sql(" inner join ")
                .sql(parent.ctx.path.getType().getTableName(sqlClient.getMetadataStrategy()))
                .sql(" tb_2_ on ");
        builder.enter(AbstractSqlBuilder.ScopeType.AND);
        int size = sourceGetters.size();
        for (int i = 0; i < size; i++) {
            builder.separator().sql("tb_1_.").sql(sourceGetters.get(i))
                    .sql(" = tb_2_.")
                    .sql(parent.targetGetters.get(i));
        }
        builder.leave();
        builder.enter(SqlBuilder.ScopeType.WHERE);
        parent.addPredicates(builder, args, 2);
        builder.leave();
        return find(null, builder, null);
    }

    @SuppressWarnings("unchecked")
    private Set<Tuple2<Object, Object>> find(
            Object onlyOneSourceId,
            SqlBuilder builder,
            @Nullable QueryReason optionalQueryReason
    ) {
        Tuple3<String, List<Object>, List<Integer>> tuple = builder.build();
        Reader<Object> sourceIdReader;
        Reader<Object> targetIdReader;
        if (path.getProp() != null) {
            if (onlyOneSourceId == null) {
                sourceIdReader = (Reader<Object>) sqlClient.getReader(path.getProp().getDeclaringType().getIdProp());
            } else {
                sourceIdReader = null;
            }
            targetIdReader = (Reader<Object>) sqlClient.getReader(path.getProp().getTargetType().getIdProp());
        } else {
            if (onlyOneSourceId == null) {
                sourceIdReader = (Reader<Object>) sqlClient.getReader(path.getBackProp().getTargetType().getIdProp());
            } else {
                sourceIdReader = null;
            }
            targetIdReader = (Reader<Object>) sqlClient.getReader(path.getBackProp().getDeclaringType().getIdProp());
        }
        return sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        tuple.get_1(),
                        tuple.get_2(),
                        tuple.get_3(),
                        ExecutionPurpose.command(
                                optionalQueryReason != null ?
                                        optionalQueryReason :
                                        queryReason
                        ),
                        exceptionTranslator,
                        null,
                        (stmt, args) -> {
                            Reader.Context ctx = new Reader.Context(null, sqlClient);
                            Set<Tuple2<Object, Object>> idTuples = new LinkedHashSet<>();
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    ctx.resetCol();
                                    Object sourceId = sourceIdReader != null ?
                                            sourceIdReader.read(rs, ctx) :
                                            onlyOneSourceId;
                                    Object targetId = targetIdReader.read(rs, ctx);
                                    idTuples.add(new Tuple2<>(sourceId, targetId));
                                }
                            }
                            return idTuples;
                        }
                )
        );
    }

    final void connect(IdPairs idPairs) {
        if (idPairs.tuples().isEmpty()) {
            return;
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        builder.sql("insert into ").sql(middleTable.getTableName()).enter(BatchSqlBuilder.ScopeType.TUPLE);
        for (ValueGetter getter : getters) {
            builder.separator().sql(getter);
        }
        builder.leave();
        builder.sql(" values").enter(BatchSqlBuilder.ScopeType.TUPLE);
        for (ValueGetter getter : getters) {
            builder.separator().variable(getter);
        }
        builder.leave();
        int rowCount = execute(
                builder,
                idPairs.tuples(),
                (ex, ctx) -> translateConnectException(ex, ctx, idPairs.tuples())
        );
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    final int[] connectIfNecessary(IdPairs idPairs) {
        if (idPairs.tuples().isEmpty()) {
            return EMPTY_ROW_COUNTS;
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        sqlClient.getDialect().upsert(new UpsertContextImpl(builder));
        int[] rowCounts = executeImpl(
                builder,
                idPairs.tuples(),
                (ex, args) -> translateConnectException(ex, args, idPairs.tuples())
        );
        AffectedRows.add(affectedRowCount, path, sumRowCount(rowCounts));
        return rowCounts;
    }

    final void disconnect(IdPairs idPairs) {
        if (idPairs.isEmpty()) {
            return;
        }
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        addOperation(builder, true);
        builder.enter(BatchSqlBuilder.ScopeType.WHERE);
        for (ValueGetter getter : referenceGetters) {
            builder.separator()
                    .sql(getter)
                    .sql(" = ")
                    .variable(getter);
        }
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        int rowCount = execute(builder, idPairs.tuples(), null);
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    final void disconnect(Collection<Object> ids) {
        disconnect(DisconnectionArgs.delete(ids, null).withTrigger(true));
    }

    final void disconnect(DisconnectionArgs args) {
        if (args.isEmpty() || disconnectingType == DisconnectingType.NONE) {
            return;
        }
        if (queryReason != QueryReason.TUPLE_IS_UNSUPPORTED && queryReason != QueryReason.NONE) {
            Set<Tuple2<Object, Object>> tuples = find(args);
            disconnect(IdPairs.of(tuples));
            if (args.fireEvents && trigger != null) {
                for (Tuple2<Object, Object> tuple : tuples) {
                    fireDelete(tuple.get_1(), tuple.get_2());
                }
            }
            return;
        }
        if (this.targetGetters.size() == 1 &&
                sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
            BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
            addOperation(builder, false);
            builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
            addPredicate(builder, parent, args);
            addLogicalDeletedPredicate(builder);
            addFilterPredicate(builder);
            builder.leave();
            int rowCount = execute(
                    builder,
                    args.deletedIds != null ?
                            args.deletedIds:
                            args.retainedIdPairs.entries(),
                    null
            );
            AffectedRows.add(affectedRowCount, path, rowCount);
            return;
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperation(builder, false);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        addPredicate(builder, parent, args);
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        int rowCount = execute(builder);
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    final void disconnectExcept(IdPairs.Retain idPairs) {
        Collection<Tuple2<Object, Collection<Object>>> entries = idPairs.entries();
        if (entries.isEmpty()) {
            return;
        }
        if (idPairs.entries().size() == 1) {
            Tuple2<Object, Collection<Object>> entry = entries.iterator().next();
            disconnectExceptBySimpleInPredicate(entry.get_1(), entry.get_2());
        } else if (targetGetters.size() == 1 && sqlClient.getDialect().isAnyEqualityOfArraySupported()) {
            disconnectExceptByBatch(idPairs);
        } else {
            disconnectExceptByComplexInPredicate(idPairs);
        }
    }

    private void disconnectExceptByBatch(IdPairs idPairs) {
        BatchSqlBuilder builder = new BatchSqlBuilder(sqlClient);
        addOperation(builder, false);
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                sourceGetters,
                targetGetters
        );
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        int rowCount = execute(builder, idPairs.entries(), null);
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    private void disconnectExceptBySimpleInPredicate(Object sourceId, Collection<Object> targetIds) {
        AstContext astContext = new AstContext(sqlClient);
        SqlBuilder builder = new SqlBuilder(astContext);
        addOperation(builder, false);
        builder.enter(SqlBuilder.ScopeType.WHERE);
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                sourceGetters,
                targetGetters,
                sourceId,
                targetIds
        );
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        int rowCount = execute(builder);
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    private void disconnectExceptByComplexInPredicate(IdPairs idPairs) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        addOperation(builder, false);
        builder.enter(SqlBuilder.ScopeType.WHERE);
        ExclusiveIdPairPredicates.addPredicates(
                builder,
                sourceGetters,
                targetGetters,
                idPairs
        );
        addLogicalDeletedPredicate(builder);
        addFilterPredicate(builder);
        builder.leave();
        int rowCount = execute(builder);
        AffectedRows.add(affectedRowCount, path, rowCount);
    }

    private void addOperation(AbstractSqlBuilder<?> builder, boolean ignoreAlias) {
        if (disconnectingType == DisconnectingType.LOGICAL_DELETE) {
            builder.sql("update ").sql(middleTable.getTableName());
            if (!ignoreAlias && alias != null) {
                builder.sql(" ").sql(alias);
            }
            builder.enter(AbstractSqlBuilder.ScopeType.SET);
            builder.logicalDeleteAssignment(
                    middleTable.getLogicalDeletedInfo(),
                    null,
                    null
            );
            builder.leave();
        } else {
            builder.sql("delete");
            if (!ignoreAlias && alias != null && sqlClient.getDialect().isDeletedAliasRequired()) {
                builder.sql(" ").sql(alias);
            }
            builder.sql(" from ")
                    .sql(middleTable.getTableName());
            if (!ignoreAlias && alias != null) {
                builder.sql(" ").sql(alias);
            }
        }
    }

    private void addPredicate(
            AbstractSqlBuilder<?> builder,
            ChildTableOperator parent,
            DisconnectionArgs args
    ) {
        if (parent == null) {
            if (args.deletedIds != null) {
                if (builder instanceof BatchSqlBuilder) {
                    BatchSqlBuilder batchSqlBuilder = (BatchSqlBuilder) builder;
                    int size = sourceGetters.size();
                    builder.enter(size == 1 ? AbstractSqlBuilder.ScopeType.NULL : AbstractSqlBuilder.ScopeType.AND);
                    for (ValueGetter sourceGetter : sourceGetters) {
                        batchSqlBuilder.separator()
                                .sql(sourceGetter)
                                .sql(" = ")
                                .variable(sourceGetter);
                    }
                    builder.leave();
                } else {
                    ComparisonPredicates.renderIn(
                            false,
                            sourceGetters,
                            args.deletedIds,
                            (SqlBuilder) builder
                    );
                }
            } else {
                disconnect(args.retainedIdPairs);
            }
            return;
        }
        builder.sql("exists ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
        builder.sql("select * from ")
                .sql(path.getParent().getType()
                .getTableName(sqlClient.getMetadataStrategy()))
                .sql(" tb_2_");
        builder.enter(AbstractSqlBuilder.ScopeType.WHERE);
        int size = sourceGetters.size();
        builder.enter(size == 1 ? AbstractSqlBuilder.ScopeType.NULL : AbstractSqlBuilder.ScopeType.AND);
        for (int i = 0; i < size; i++) {
            builder.separator()
                    .sql("tb_1_.")
                    .sql(sourceGetters.get(i))
                    .sql(" = ")
                    .sql("tb_2_.")
                    .sql(parent.targetGetters.get(i));
        }
        builder.leave();
        parent.addPredicates(builder, args, 2);
        builder.leave();
        builder.leave();
    }

    private void addLogicalDeletedPredicate(AbstractSqlBuilder<?> builder) {
        if (disconnectingType != DisconnectingType.LOGICAL_DELETE) {
            return;
        }
        LogicalDeletedInfo logicalDeletedInfo = middleTable.getLogicalDeletedInfo();
        if (logicalDeletedInfo == null) {
            return;
        }
        builder.separator();
        LogicalDeletedInfo.Action action = logicalDeletedInfo.getAction();
        if (action instanceof LogicalDeletedInfo.Action.Eq) {
            LogicalDeletedInfo.Action.Eq eq = (LogicalDeletedInfo.Action.Eq) action;
            Object value = eq.getValue();
            value = Variables.process(value, logicalDeletedInfo.getType(), builder.sqlClient());
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" = ").rawVariable(value);
        } else if (action instanceof LogicalDeletedInfo.Action.Ne) {
            LogicalDeletedInfo.Action.Ne ne = (LogicalDeletedInfo.Action.Ne) action;
            Object value = ne.getValue();
            value = Variables.process(value, logicalDeletedInfo.getType(), builder.sqlClient());
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" <> ").rawVariable(value);
        } else if (action instanceof LogicalDeletedInfo.Action.IsNull) {
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" is null");
        } else if (action instanceof LogicalDeletedInfo.Action.IsNotNull) {
            builder.sql(logicalDeletedInfo.getColumnName()).sql(" is not null");
        }
    }

    private void addFilterPredicate(AbstractSqlBuilder<?> builder) {
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();
        if (filterInfo == null) {
            return;
        }
        builder.separator().sql(filterInfo.getColumnName());
        if (filterInfo.getValues().size() == 1) {
            builder.sql(" = ").rawVariable(filterInfo.getValues().get(0));
        } else {
            builder.sql(" in ").enter(SqlBuilder.ScopeType.LIST);
            for (Object value : filterInfo.getValues()) {
                builder.separator().rawVariable(value);
            }
            builder.leave();
        }
    }

    private boolean isUpsertUsed() {
        Dialect dialect = sqlClient.getDialect();
        return dialect.isUpsertSupported() && trigger == null;
    }

    private class UpsertContextImpl implements Dialect.UpsertContext {

        private final BatchSqlBuilder builder;

        UpsertContextImpl(BatchSqlBuilder builder) {
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
        public boolean hasGeneratedId() {
            return false;
        }

        @Override
        public boolean isUpdateIgnored() {
            return true;
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public List<ValueGetter> getConflictGetters() {
            return getters;
        }

        @Override
        public Dialect.UpsertContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpsertContext sql(ValueGetter getter) {
            builder.sql(getter);
            return this;
        }

        @Override
        public Dialect.UpsertContext enter(AbstractSqlBuilder.ScopeType type) {
            builder.enter(type);
            return this;
        }

        @Override
        public Dialect.UpsertContext separator() {
            builder.separator();
            return this;
        }

        @Override
        public Dialect.UpsertContext leave() {
            builder.leave();
            return this;
        }

        @Override
        public Dialect.UpsertContext appendTableName() {
            builder.sql(middleTable.getTableName());
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertedColumns(String prefix) {
            for (ValueGetter getter : getters) {
                builder.separator().sql(prefix).sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendConflictColumns() {
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendInsertingValues() {
            for (ValueGetter getter : getters) {
                builder.separator().variable(getter);
            }
            return this;
        }

        @Override
        public Dialect.UpsertContext appendUpdatingAssignments(String prefix, String suffix) {
            return this;
        }

        @Override
        public Dialect.UpsertContext appendOptimisticLockCondition(String sourceTablePrefix) {
            return this;
        }

        @Override
        public Dialect.UpsertContext appendGeneratedId() {
            return this;
        }
    }

    private class DeletedGetter implements ValueGetter, GetterMetadata {

        @Override
        public Object get(Object value) {
            return middleTable.getLogicalDeletedInfo().allocateInitializedValue();
        }

        @Override
        public GetterMetadata metadata() {
            return this;
        }

        @Override
        public ImmutableProp getValueProp() {
            return null;
        }

        @Override
        public @Nullable String getColumnName() {
            return middleTable.getLogicalDeletedInfo().getColumnName();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public boolean isJson() {
            return false;
        }

        @Override
        public boolean hasDefaultValue() {
            return false;
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
        public Class<?> getSqlType() {
            Class<?> type = middleTable.getLogicalDeletedInfo().getType();
            ScalarProvider<?, ?> provider = sqlClient.getScalarProvider(type);
            return provider != null ? provider.getSqlType() : type;
        }

        @Override
        public String getSqlTypeName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renderTo(AbstractSqlBuilder<?> builder) {
            builder.sql(middleTable.getLogicalDeletedInfo().getColumnName());
        }
    }

    private class FilterGetter implements ValueGetter, GetterMetadata {

        @Override
        public Object get(Object value) {
            return middleTable.getFilterInfo().getValues().get(0);
        }

        @Override
        public GetterMetadata metadata() {
            return this;
        }

        @Override
        public ImmutableProp getValueProp() {
            return null;
        }

        @Override
        public @Nullable String getColumnName() {
            return middleTable.getFilterInfo().getColumnName();
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public boolean isJson() {
            return false;
        }

        @Override
        public boolean hasDefaultValue() {
            return false;
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

        @Override
        public Class<?> getSqlType() {
            return String.class;
        }

        @Override
        public String getSqlTypeName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renderTo(AbstractSqlBuilder<?> builder) {
            builder.sql(middleTable.getFilterInfo().getColumnName());
        }
    }
    
    private void fireInsert(Object sourceId, Object targetId) {
        ImmutableProp prop = path.getProp();
        if (prop != null) {
            trigger.insertMiddleTable(prop, sourceId, targetId);
        } else {
            trigger.insertMiddleTable(path.getBackProp(), targetId, sourceId);
        }
    }

    private void fireDelete(Object sourceId, Object targetId) {
        ImmutableProp prop = path.getProp();
        if (prop != null) {
            trigger.deleteMiddleTable(prop, sourceId, targetId);
        } else {
            trigger.deleteMiddleTable(path.getBackProp(), targetId, sourceId);
        }
    }

    private Exception translateConnectException(
            SQLException ex,
            ExceptionTranslator.Args args,
            Collection<Tuple2<Object, Object>> idTuples
    ) {
        String state = ex.getSQLState();
        if (state == null || !state.startsWith("23")) {
            return convertFinalException(ex, args);
        }
        int[] affectedRowCounts;
        if (ex instanceof BatchUpdateException) {
            affectedRowCounts = ((BatchUpdateException)ex).getUpdateCounts();
        } else {
            affectedRowCounts = SINGLE_ERROR_ROW_COUNTS;
        }
        MiddleTableInvestigator investigator = new MiddleTableInvestigator(
                ex,
                affectedRowCounts,
                Investigators.toInvestigatorSqlClient(sqlClient, args),
                con,
                path,
                idTuples
        );
        Exception investigatedException = investigator.investigate();
        if (investigatedException == null) {
            investigatedException = ex;
        }
        return convertFinalException(investigatedException, args);
    }

    private Exception convertFinalException(Exception ex, ExceptionTranslator.Args args) {
        if (this.exceptionTranslator == null) {
            return ex;
        }
        return this.exceptionTranslator.translate(ex, args);
    }
}
