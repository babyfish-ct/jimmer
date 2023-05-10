package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

class MiddleTableOperator {

    private final JSqlClientImplementor sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    private final boolean isBackProp;

    private final MiddleTable middleTable;

    private final Expression<?> sourceIdExpression;

    private final Expression<?> targetIdExpression;

    private final MutationTrigger trigger;

    private MiddleTableOperator(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableProp prop,
            boolean isBackProp,
            MiddleTable middleTable,
            MutationTrigger trigger
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.isBackProp = isBackProp;
        this.middleTable = middleTable;
        if (isBackProp) {
            this.sourceIdExpression = Expression.any().nullValue(prop.getTargetType().getIdProp().getElementClass());
            this.targetIdExpression = Expression.any().nullValue(prop.getDeclaringType().getIdProp().getElementClass());
        } else {
            this.sourceIdExpression = Expression.any().nullValue(prop.getDeclaringType().getIdProp().getElementClass());
            this.targetIdExpression = Expression.any().nullValue(prop.getTargetType().getIdProp().getElementClass());
        }
        this.trigger = trigger;
    }

    public static MiddleTableOperator tryGet(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableProp prop,
            MutationTrigger trigger
    ) {
        return tryGetImpl(sqlClient, con, prop, false, trigger);
    }

    public static MiddleTableOperator tryGetByBackProp(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableProp backProp,
            MutationTrigger trigger
    ) {
        return tryGetImpl(sqlClient, con, backProp, true, trigger);
    }

    private static MiddleTableOperator tryGetImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            ImmutableProp prop,
            boolean isPropBack,
            MutationTrigger trigger
    ) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null && prop.isRemote()) {
            return null;
        }
        MetadataStrategy strategy = sqlClient.getMetadataStrategy();
        if (mappedBy != null) {
            Storage storage = mappedBy.getStorage(strategy);
            if (storage instanceof MiddleTable) {
                MiddleTable middleTable = isPropBack ? (MiddleTable) storage : ((MiddleTable) storage).getInverse();
                return new MiddleTableOperator(
                        sqlClient, con, prop, isPropBack, middleTable, trigger
                );
            }
        } else {
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof MiddleTable) {
                MiddleTable middleTable = isPropBack ? ((MiddleTable) storage).getInverse() : (MiddleTable) storage;
                return new MiddleTableOperator(
                        sqlClient, con, prop, isPropBack, middleTable, trigger
                );
            }
        }
        return null;
    }

    List<Object> getTargetIds(Object id) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .enter(SqlBuilder.ScopeType.SELECT)
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .from()
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE)
                .definition(null, middleTable.getColumnDefinition(), true)
                .sql(" = ")
                .variable(id)
                .leave();
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Collections.singletonList(targetIdExpression),
                ExecutionPurpose.MUTATE
        );
    }

    private IdPairReader filterReader(IdPairReader reader) {
        if (!reader.isReadable()) {
            return new TupleReader(Collections.emptyList());
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .enter(SqlBuilder.ScopeType.SELECT)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .from()
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE)
                .enter(SqlBuilder.ScopeType.TUPLE)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .sql(" in ").enter(SqlBuilder.ScopeType.LIST);
        while (reader.read()) {
            builder.separator();
            builder
                    .enter(SqlBuilder.ScopeType.TUPLE)
                    .variable(reader.sourceId())
                    .separator()
                    .variable(reader.targetId())
                    .leave();
        }
        builder.leave().leave();

        reader.reset();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        List<Tuple2<Object, Object>> tuples = Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Arrays.asList(sourceIdExpression, targetIdExpression),
                ExecutionPurpose.MUTATE
        );
        return new TupleReader(tuples);
    }

    IdPairReader getIdPairReader(Collection<Object> sourceIds) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .enter(SqlBuilder.ScopeType.SELECT)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .from()
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE)
                .definition(null, middleTable.getColumnDefinition(), true)
                .sql(" in ")
                .enter(SqlBuilder.ScopeType.LIST);
        for (Object sourceId : sourceIds) {
            builder.separator().variable(sourceId);
        }
        builder.leave().leave();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        List<Tuple2<Object, Object>> tuples = Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Arrays.asList(sourceIdExpression, targetIdExpression),
                ExecutionPurpose.MUTATE
        );
        return new TupleReader(tuples);
    }

    int addTargetIds(Object sourceId, Collection<Object> targetIds) {

        if (targetIds.isEmpty()) {
            return 0;
        }

        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        return add(new OneToManyReader(sourceId, set));
    }

    int add(IdPairReader reader) {

        if (!reader.isReadable()) {
            return 0;
        }

        tryPrepareEvent(true, reader);

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("insert into ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.TUPLE)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave();
        if (sqlClient.getDialect().isMultiInsertionSupported()) {
            builder.enter(SqlBuilder.ScopeType.VALUES);
            while (reader.read()) {
                builder
                        .separator()
                        .enter(SqlBuilder.ScopeType.TUPLE)
                        .variable(reader.sourceId())
                        .separator()
                        .variable(reader.targetId())
                        .leave();
            }
            builder.leave();
        } else {
            builder.sql(" ");
            String fromConstant = sqlClient.getDialect().getConstantTableName();
            if (fromConstant != null) {
                fromConstant = " from " + fromConstant;
            }
            builder.enter("?union all?");
            while (reader.read()) {
                builder
                        .separator()
                        .enter(SqlBuilder.ScopeType.SELECT)
                        .variable(reader.sourceId())
                        .separator()
                        .variable(reader.targetId())
                        .leave();
                if (fromConstant != null) {
                    builder.sql(fromConstant);
                }
            }
            builder.leave();
        }
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
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

    int remove(Object sourceId, Collection<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return 0;
        }
        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        return remove(new OneToManyReader(sourceId, set));
    }

    int remove(IdPairReader reader) {
        return remove(reader, false);
    }

    int remove(IdPairReader reader, boolean checkExistence) {

        if (!reader.isReadable()) {
            return 0;
        }
        if (checkExistence) {
            reader = filterReader(reader);
            if (!reader.isReadable()) {
                return 0;
            }
        }

        tryPrepareEvent(false, reader);

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE)
                .enter(SqlBuilder.ScopeType.TUPLE)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .sql(" in ")
                .enter(SqlBuilder.ScopeType.LIST);
        while (reader.read()) {
            builder
                    .separator()
                    .enter(SqlBuilder.ScopeType.TUPLE)
                    .variable(reader.sourceId())
                    .separator()
                    .variable(reader.targetId())
                    .leave();
        }
        builder.leave().leave();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
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

    int setTargetIds(Object sourceId, Collection<Object> targetIds) {

        Set<Object> oldTargetIds = new LinkedHashSet<>(getTargetIds(sourceId));

        Set<Object> addingTargetIds = new LinkedHashSet<>(targetIds);
        addingTargetIds.removeAll(oldTargetIds);

        Set<Object> removingTargetIds = new LinkedHashSet<>(oldTargetIds);
        removingTargetIds.removeAll(targetIds);

        return remove(sourceId, removingTargetIds) + addTargetIds(sourceId, addingTargetIds);
    }

    public int removeBySourceIds(Collection<Object> sourceIds) {
        if (trigger != null) {
            IdPairReader reader = getIdPairReader(sourceIds);
            return remove(reader);
        }
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE)
                .definition(null, middleTable.getColumnDefinition(), true)
                .sql(" in ")
                .enter(SqlBuilder.ScopeType.LIST);
        for (Object id : sourceIds) {
            builder.separator().variable(id);
        }
        builder.leave().leave();

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
                                ExecutionPurpose.DELETE,
                                null,
                                PreparedStatement::executeUpdate
                        )
                );
    }

    private void tryPrepareEvent(boolean insert, IdPairReader reader) {

        MutationTrigger trigger = this.trigger;
        if (trigger == null) {
            return;
        }

        while (reader.read()) {
            Object sourceId = reader.sourceId();
            Object targetId = reader.targetId();
            if (isBackProp) {
                if (insert) {
                    trigger.insertMiddleTable(prop, targetId, sourceId);
                } else {
                    trigger.deleteMiddleTable(prop, targetId, sourceId);
                }
            } else {
                if (insert) {
                    trigger.insertMiddleTable(prop, sourceId, targetId);
                } else {
                    trigger.deleteMiddleTable(prop, sourceId, targetId);
                }
            }
        }

        reader.reset();
    }

    interface IdPairReader {
        void reset();
        boolean read();
        boolean isReadable();
        Object sourceId();
        Object targetId();
    }

    private static class OneToManyReader implements IdPairReader {

        private final Object sourceId;

        private final Collection<Object> targetIds;

        private Iterator<Object> targetIdItr;

        private Object currentTargetId;

        OneToManyReader(Object sourceId, Collection<Object> targetIds) {
            this.sourceId = sourceId;
            this.targetIds = targetIds;
            this.targetIdItr = targetIds.iterator();
        }

        @Override
        public void reset() {
            this.targetIdItr = targetIds.iterator();
        }

        @Override
        public boolean read() {
            if (targetIdItr.hasNext()) {
                currentTargetId = targetIdItr.next();
                return true;
            }
            return false;
        }

        @Override
        public boolean isReadable() {
            return targetIdItr.hasNext();
        }

        @Override
        public Object sourceId() {
            return sourceId;
        }

        @Override
        public Object targetId() {
            return currentTargetId;
        }
    }

    public static class TupleReader implements MiddleTableOperator.IdPairReader {

        private final Collection<Tuple2<Object, Object>> idTuples;

        private Iterator<Tuple2<Object, Object>> idTupleItr;

        private Tuple2<Object, Object> currentIdPair;

        public TupleReader(Collection<Tuple2<Object, Object>> idTuples) {
            this.idTuples = idTuples;
            idTupleItr = idTuples.iterator();
        }

        @Override
        public void reset() {
            idTupleItr = idTuples.iterator();
        }

        @Override
        public boolean read() {
            if (idTupleItr.hasNext()) {
                currentIdPair = idTupleItr.next();
                return true;
            }
            return false;
        }

        @Override
        public boolean isReadable() {
            return idTupleItr.hasNext();
        }

        @Override
        public Object sourceId() {
            return currentIdPair.get_1();
        }

        @Override
        public Object targetId() {
            return currentIdPair.get_2();
        }
    }
}
