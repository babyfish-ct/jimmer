package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.table.JoinTableFilters;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.meta.JoinTableFilterInfo;
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

    private final boolean isActive;

    private final boolean hasFilter;

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
        boolean hasMiddleTableFilter = (
                middleTable.getLogicalDeletedInfo() != null &&
                        sqlClient.getFilters().getBehavior(prop) != LogicalDeletedBehavior.IGNORED
        ) || middleTable.getFilterInfo() != null;
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.isActive = sqlClient.getEntityManager().isActiveMiddleTableProp(prop);
        this.isBackProp = isBackProp;
        this.middleTable = middleTable;
        if (isBackProp) {
            this.sourceIdExpression = Expression.any().nullValue(prop.getTargetType().getIdProp().getElementClass());
            this.targetIdExpression = Expression.any().nullValue(prop.getDeclaringType().getIdProp().getElementClass());
            this.hasFilter = sqlClient.getFilters().getFilter(prop.getDeclaringType()) != null || hasMiddleTableFilter;
        } else {
            this.sourceIdExpression = Expression.any().nullValue(prop.getDeclaringType().getIdProp().getElementClass());
            this.targetIdExpression = Expression.any().nullValue(prop.getTargetType().getIdProp().getElementClass());
            this.hasFilter = sqlClient.getFilters().getFilter(prop.getTargetType()) != null || hasMiddleTableFilter;
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

    static MiddleTableOperator tryGetByBackProp(
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

    boolean isActive() {
        return isActive;
    }

    boolean isLogicalDeletionSupported() {
        return middleTable.getLogicalDeletedInfo() != null;
    }

    boolean isDeletedWhenEndpointIsLogicallyDeleted() {
        return middleTable.isDeletedWhenEndpointIsLogicallyDeleted();
    }

    boolean isCascadeDeletedBySource() {
        return middleTable.isCascadeDeletedBySource();
    }

    boolean isCascadeDeletedByTarget() {
        return middleTable.isCascadeDeletedByTarget();
    }

    List<Object> getTargetIds(Object id) {

        if (hasFilter) {
            return getTargetIdsByDsl(id);
        }

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .enter(SqlBuilder.ScopeType.SELECT)
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .from()
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderPredicates(
                false,
                middleTable.getColumnDefinition(),
                Collections.singleton(id),
                builder
        );
        builder.leave();
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

    private List<Object> getTargetIdsByDsl(Object id) {
        ImmutableType targetType = prop.getTargetType();
        MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(sqlClient, targetType, ExecutionPurpose.MUTATE, FilterLevel.DEFAULT);
        TableImplementor<?> table = query.getTableImplementor();
        query.where(table.inverseGetAssociatedId(prop).eq(id));
        return query.select(table.get(targetType.getIdProp())).execute(con);
    }

    private Collection<Tuple2<?, ?>> filterTuples(Collection<Tuple2<?, ?>> tuples) {
        if (tuples.isEmpty()) {
            return tuples;
        }

        if (hasFilter) {
            return filterTuplesByDsl(tuples);
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
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderTuplePredicates(
                false,
                middleTable.getColumnDefinition(),
                middleTable.getTargetColumnDefinition(),
                tuples,
                builder
        );
        builder.leave();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Arrays.asList(sourceIdExpression, targetIdExpression),
                ExecutionPurpose.MUTATE
        );
    }

    @SuppressWarnings("unchecked")
    private List<Tuple2<?, ?>> filterTuplesByDsl(Collection<Tuple2<?, ?>> tuples) {
        ImmutableType targetType = prop.getTargetType();
        MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(sqlClient, targetType, ExecutionPurpose.MUTATE, FilterLevel.DEFAULT);
        TableImplementor<?> table = query.getTableImplementor();
        ImmutableProp sourceIdProp = prop.getDeclaringType().getIdProp();
        query.where(
                Expression.tuple(
                        table.inverseJoinImplementor(prop).get(sourceIdProp),
                        table.get(targetType.getIdProp())
                ).in((List<Tuple2<Object, Object>>)(List<?>)tuples)
        );
        return (List<Tuple2<?, ?>>)(List<?>)query
                .select(
                        table.inverseJoinImplementor(prop).<Expression<Object>>get(sourceIdProp),
                        table.<Expression<Object>>get(targetType.getIdProp())
                )
                .execute(con);
    }

    private List<Tuple2<?, ?>> getTuples(Collection<Object> sourceIds, boolean skipLogicalDeletedRows) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .enter(SqlBuilder.ScopeType.SELECT)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition())
                .leave()
                .from()
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderPredicates(
                false,
                middleTable.getColumnDefinition(),
                sourceIds,
                builder
        );
        if (skipLogicalDeletedRows) {
            LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();
            if (deletedInfo != null) {
                builder.separator();
                JoinTableFilters.render(sqlClient.getFilters().getBehavior(prop), deletedInfo, null, builder);
            }
        }
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();
        if (filterInfo != null) {
            builder.separator();
            JoinTableFilters.render(filterInfo, null, builder);
        }
        builder.leave();

        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Arrays.asList(sourceIdExpression, targetIdExpression),
                ExecutionPurpose.MUTATE
        );
    }

    int addTargetIds(Object sourceId, Collection<Object> targetIds) {

        if (targetIds.isEmpty()) {
            return 0;
        }

        Set<Tuple2<?, ?>> tuples = new LinkedHashSet<>((targetIds.size() * 4 + 2) / 3);
        for (Object targetId : targetIds) {
            tuples.add(new Tuple2<>(sourceId, targetId));
        }

        return add(tuples);
    }

    int add(Collection<Tuple2<?, ?>> tuples) {

        if (middleTable.isReadonly()) {
            throw new ExecutionException(
                    "The association \"" +
                            prop +
                            "\" cannot be changed because its \"@JoinTable\" is readonly"
            );
        }

        if (tuples.isEmpty()) {
            return 0;
        }

        tryPrepareEvent(true, tuples);

        LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("insert into ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.TUPLE)
                .definition(middleTable.getColumnDefinition())
                .separator()
                .definition(middleTable.getTargetColumnDefinition());
        if (deletedInfo != null) {
            builder.separator().sql(deletedInfo.getColumnName());
        }
        if (filterInfo != null) {
            builder.separator().sql(filterInfo.getColumnName());
        }
        builder.leave();
        if (sqlClient.getDialect().isMultiInsertionSupported()) {
            builder.enter(SqlBuilder.ScopeType.VALUES);
            for (Tuple2<?, ?> tuple : tuples) {
                builder
                        .separator()
                        .enter(SqlBuilder.ScopeType.TUPLE)
                        .variable(tuple.get_1())
                        .separator()
                        .variable(tuple.get_2());
                if (deletedInfo != null) {
                    builder
                            .separator()
                            .variable(deletedInfo.allocateInitializedValue());
                }
                if (filterInfo != null) {
                    builder
                            .separator()
                            .variable(filterInfo.getValues().get(0));
                }
                builder.leave();
            }
            builder.leave();
        } else {
            builder.sql(" ");
            String fromConstant = sqlClient.getDialect().getConstantTableName();
            if (fromConstant != null) {
                fromConstant = " from " + fromConstant;
            }
            builder.enter("?union all?");
            for (Tuple2<?, ?> tuple : tuples) {
                builder
                        .separator()
                        .enter(SqlBuilder.ScopeType.SELECT)
                        .variable(tuple.get_1())
                        .separator()
                        .variable(tuple.get_2());
                if (deletedInfo != null) {
                    builder
                            .separator()
                            .variable(deletedInfo.allocateInitializedValue());
                }
                if (filterInfo != null) {
                    builder
                            .separator()
                            .variable(filterInfo.getValues().get(0));
                }
                builder.leave();
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
        Set<Tuple2<?, ?>> tuples = new LinkedHashSet<>((targetIds.size() * 4 + 2) / 3);
        for (Object targetId : targetIds) {
            tuples.add(new Tuple2<>(sourceId, targetId));
        }

        return remove(tuples);
    }

    int remove(Collection<Tuple2<?, ?>> tuples) {
        return remove(tuples, false);
    }

    int remove(Collection<Tuple2<?, ?>> tuples, boolean checkExistence) {

        if (tuples.isEmpty()) {
            return 0;
        }
        if (checkExistence) {
            tuples = filterTuples(tuples);
            if (tuples.isEmpty()) {
                return 0;
            }
        }

        tryPrepareEvent(false, tuples);

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderTuplePredicates(
                false,
                middleTable.getColumnDefinition(),
                middleTable.getTargetColumnDefinition(),
                tuples,
                builder
        );
        builder.leave();

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

    int hide(Collection<Tuple2<?, ?>> tuples) {
        return hide(tuples, false);
    }

    int hide(Collection<Tuple2<?, ?>> tuples, boolean checkExistence) {

        if (tuples.isEmpty()) {
            return 0;
        }
        if (checkExistence) {
            tuples = filterTuples(tuples);
            if (tuples.isEmpty()) {
                return 0;
            }
        }

        tryPrepareEvent(false, tuples);

        LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("update ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.SET)
                .sql(deletedInfo.getColumnName())
                .sql(" = ");
        Object deletedValue = prop.getLogicalDeletedValueGenerator(sqlClient).generate();
        if (deletedValue != null) {
            builder.variable(deletedValue);
        } else {
            builder.nullVariable(deletedInfo.getType());
        }
        builder
                .leave()
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderTuplePredicates(
                false,
                middleTable.getColumnDefinition(),
                middleTable.getTargetColumnDefinition(),
                tuples,
                builder
        );
        builder.leave();

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

    public int logicallyDeleteBySourceIds(Collection<Object> sourceIds) throws DeletionPreventedException {

        boolean deletionBySourcePrevented = middleTable.isDeletionBySourcePrevented();
        if (trigger != null || deletionBySourcePrevented) {
            List<Tuple2<?, ?>> tuples = getTuples(sourceIds, true);
            if (deletionBySourcePrevented && !tuples.isEmpty()) {
                throw new DeletionPreventedException(middleTable, tuples);
            }
            return hide(tuples);
        }

        LogicalDeletedInfo deletedInfo = middleTable.getLogicalDeletedInfo();
        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("update ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.SET)
                .sql(deletedInfo.getColumnName())
                .sql(" = ");
        Object deletedValue = prop.getLogicalDeletedValueGenerator(sqlClient).generate();
        if (deletedValue != null) {
            builder.variable(deletedValue);
        } else {
            builder.nullVariable(deletedInfo.getType());
        }
        builder.leave()
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderPredicates(
                false,
                middleTable.getColumnDefinition(),
                sourceIds,
                builder
        );
        builder.separator();
        JoinTableFilters.render(sqlClient.getFilters().getBehavior(prop), deletedInfo, null, builder);
        if (filterInfo != null) {
            builder.separator();
            JoinTableFilters.render(filterInfo, null, builder);
        }
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
                                ExecutionPurpose.DELETE,
                                null,
                                PreparedStatement::executeUpdate
                        )
                );
    }

    public int physicallyDeleteBySourceIds(Collection<Object> sourceIds) throws DeletionPreventedException {

        boolean deletionBySourcePrevented = middleTable.isDeletionBySourcePrevented();
        if (trigger != null || deletionBySourcePrevented) {
            List<Tuple2<?, ?>> tuples = getTuples(sourceIds, false);
            if (deletionBySourcePrevented && !tuples.isEmpty()) {
                throw new DeletionPreventedException(middleTable, tuples);
            }
            return remove(tuples);
        }

        JoinTableFilterInfo filterInfo = middleTable.getFilterInfo();

        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .enter(SqlBuilder.ScopeType.WHERE);
        NativePredicates.renderPredicates(
                false,
                middleTable.getColumnDefinition(),
                sourceIds,
                builder
        );
        if (filterInfo != null) {
            builder.separator();
            JoinTableFilters.render(filterInfo, null, builder);
        }
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
                                ExecutionPurpose.DELETE,
                                null,
                                PreparedStatement::executeUpdate
                        )
                );
    }

    private void tryPrepareEvent(boolean insert, Collection<Tuple2<?, ?>> tuples) {

        MutationTrigger trigger = this.trigger;
        if (trigger == null) {
            return;
        }

        for (Tuple2<?, ?> tuple : tuples) {
            Object sourceId = tuple.get_1();
            Object targetId = tuple.get_2();
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
    }

    static class DeletionPreventedException extends Exception {

        final MiddleTable middleTable;

        final List<Tuple2<?, ?>> tuples;

        DeletionPreventedException(MiddleTable middleTable, List<Tuple2<?, ?>> tuples) {
            this.middleTable = middleTable;
            this.tuples = Collections.unmodifiableList(tuples);
        }
    }
}
