package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.MutableRootQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageCollector;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsages;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.impl.util.JdbcOptionValidator;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Stream;

public class MutableUpdateImpl
        extends AbstractMutableStatementImpl
        implements MutableUpdate {

    private final MutableRootQueryImpl<TableEx<?>> updateQuery;

    private final boolean triggerIgnored;

    private final Map<Target, Expression<?>> assignmentMap = new LinkedHashMap<>();
    private final Set<ImmutableType> assignmentStageTypes = new LinkedHashSet<>();
    private TableLikeImplementor<?> aliasSource;
    private TypeMatchMode typeMatchMode = TypeMatchMode.AUTO;
    private JdbcOptions jdbcOptions = JdbcOptions.EMPTY;
    private boolean typeMatchPredicateApplied;
    private ImmutableType primaryAssignmentStageType;

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        this.updateQuery = MutationQuerySupport.createUpdateQuery(
                sqlClient,
                immutableType,
                this::shouldApplyImplicitDiscriminatorPredicate
        );
        this.triggerIgnored = false;
    }

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType, boolean triggerIgnored) {
        super(sqlClient, immutableType);
        this.updateQuery = MutationQuerySupport.createUpdateQuery(
                sqlClient,
                immutableType,
                this::shouldApplyImplicitDiscriminatorPredicate
        );
        this.triggerIgnored = triggerIgnored;
    }

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, TableProxy<?> table) {
        super(sqlClient, table);
        this.updateQuery = MutationQuerySupport.createUpdateQuery(
                sqlClient,
                table,
                this::shouldApplyImplicitDiscriminatorPredicate
        );
        this.triggerIgnored = false;
    }

    @Override
    public <T extends TableLike<?>> T getTable() {
        return updateQuery.getTable();
    }

    @Override
    public StatementContext getContext() {
        return updateQuery.getContext();
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
    }

    @Override
    protected boolean shouldApplyImplicitDiscriminatorPredicate(TableImplementor<?> table) {
        ImmutableType type = table.getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null && typeMatchPredicateApplied) {
            return false;
        }
        return inheritanceInfo == null ||
                inheritanceInfo.getStrategy() != InheritanceType.JOINED ||
                inheritanceInfo.getRootType() == type;
    }

    @Override
    public MutableUpdateImpl setTypeMatchMode(TypeMatchMode mode) {
        validateMutable();
        this.typeMatchMode = mode != null ? mode : TypeMatchMode.AUTO;
        return this;
    }

    @Override
    public MutableUpdateImpl jdbcFetchSize(@Nullable Integer fetchSize) {
        validateMutable();
        JdbcOptionValidator.validateLocalFetchSize(fetchSize);
        jdbcOptions = jdbcOptions.fetchSize(fetchSize);
        return this;
    }

    @Override
    public MutableUpdateImpl jdbcQueryTimeout(@Nullable Integer queryTimeout) {
        validateMutable();
        JdbcOptionValidator.validateLocalQueryTimeout(queryTimeout);
        jdbcOptions = jdbcOptions.queryTimeout(queryTimeout);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> MutableUpdateImpl set(PropExpression<X> path, X value) {
        if (value != null) {
            return set(path, Expression.any().value(value));
        }
        return set(
                path,
                Expression.nullValue(((ExpressionImplementor<X>) path).getType())
        );
    }

    @Override
    public <X> MutableUpdateImpl set(PropExpression<X> path, Expression<X> value) {
        validateMutable();
        Target target = Target.of(path, getSqlClient().getMetadataStrategy());
        if (target.table != this.getTable() &&
                target.table != this.getTableLikeImplementor() &&
                getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            throw new IllegalArgumentException(
                    "Only the primary table can be deleted when transaction trigger is supported"
            );
        }
        if (!(target.prop.isColumnDefinition())) {
            throw new IllegalArgumentException("The assigned prop expression must be mapped by database columns");
        }
        validateJoinedInheritanceAssignment(target);
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean joinedTableUpdatable = updateJoin != null && updateJoin.isJoinedTableUpdatable();
        if (!joinedTableUpdatable && (target.table != getTable() && target.table != getTableLikeImplementor())) {
            throw new IllegalArgumentException(
                    "The current dialect '" +
                            getSqlClient().getDialect().getClass().getName() +
                            "' indicates that " +
                            "only the columns of current table can be updated"
            );
        }
        if (assignmentMap.put(target, value) != null) {
            throw new IllegalStateException("Cannot update same column twice");
        }
        Literals.bind(value, path);
        return this;
    }

    private void validateJoinedInheritanceAssignment(Target target) {
        ImmutableType updateType = getTableLikeImplementor().getImmutableType();
        InheritanceInfo inheritanceInfo = updateType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            return;
        }
        ImmutableProp originalProp = target.prop.toOriginal();
        if (originalProp == inheritanceInfo.getDiscriminatorProp(updateType).toOriginal()) {
            throw new IllegalArgumentException(
                    "The discriminator property \"" +
                            target.prop +
                            "\" cannot be updated by createUpdate for joined inheritance type \"" +
                            updateType +
                            "\""
            );
        }
        ImmutableType stageType = inheritanceInfo.getTableTypeForProp(originalProp, updateType);
        if (stageType == null) {
            throw new IllegalArgumentException(
                    "Cannot update property \"" +
                            target.prop +
                            "\" by createUpdate for joined inheritance type \"" +
                            updateType +
                            "\" because it does not belong to the inheritance path of \"" +
                            originalProp.getDeclaringType() +
                            "\""
            );
        }
        if (!assignmentStageTypes.isEmpty() &&
                !assignmentStageTypes.contains(stageType) &&
                !isJoinedInheritanceMultiStageAssignmentSupported()) {
            MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
            throw new IllegalArgumentException(
                    "Cannot update property \"" +
                            target.prop +
                            "\" by createUpdate for joined inheritance type \"" +
                            updateType +
                            "\" because all assignment targets must belong to the same physical table. " +
                            "Current assignment targets table \"" +
                            stageType.getTableName(strategy) +
                            "\" but previous assignments target table \"" +
                            primaryAssignmentStageType.getTableName(strategy) +
                            "\". Updating columns in multiple database tables by one createUpdate " +
                            "for joined inheritance requires a dialect that supports multi-table update assignment"
            );
        }
        assignmentStageTypes.add(stageType);
        ImmutableType primaryStageType = primaryAssignmentStageType;
        if (primaryStageType == null || stageType.isAssignableFrom(primaryStageType)) {
            primaryAssignmentStageType = stageType;
        }
    }

    private boolean isJoinedInheritanceMultiStageAssignmentSupported() {
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        return updateJoin != null &&
                updateJoin.isJoinedTableUpdatable() &&
                updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY;
    }

    @Override
    public MutableUpdateImpl where(Predicate... predicates) {
        updateQuery.where(predicates);
        return this;
    }

    @Override
    public void whereByFilter(TableImplementor<?> tableImplementor, List<Predicate> predicates) {
        updateQuery.whereByFilter(tableImplementor, predicates);
    }

    @Override
    protected void onFrozen(AstContext astContext) {
        applyTypeMatchPredicate();
        updateQuery.freeze(astContext);
    }

    private void applyTypeMatchPredicate() {
        if (typeMatchPredicateApplied) {
            return;
        }
        typeMatchPredicateApplied = true;
        TableImplementor<?> table = getTableLikeImplementor();
        ImmutableType type = table.getImmutableType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo == null) {
            return;
        }
        TypeMatchMode resolvedMode = TypeMatchModes.resolve(type, typeMatchMode);
        if (resolvedMode == TypeMatchMode.EXACT && !type.isInstantiable()) {
            throw new ExecutionException(
                    "Cannot update inheritance entity type \"" +
                            type +
                            "\" exactly because it is abstract. Update an instantiable type or use " +
                            TypeMatchMode.POLYMORPHIC +
                            " type match mode."
            );
        }
        if (!isTypeMatchPredicateRequired(inheritanceInfo, type, resolvedMode)) {
            return;
        }
        Collection<ImmutableType> matchedTypes = matchedTypes(inheritanceInfo, type, resolvedMode);
        List<Object> values = InheritanceMutationUtils.discriminatorValues(inheritanceInfo, matchedTypes);
        if (values.isEmpty()) {
            return;
        }
        PropExpression<Object> expr = table.get(inheritanceInfo.getDiscriminatorProp(type), false);
        where(values.size() == 1 ? expr.eq(values.get(0)) : expr.in(values));
    }

    private boolean isTypeMatchPredicateRequired(
            InheritanceInfo inheritanceInfo,
            ImmutableType type,
            TypeMatchMode resolvedMode
    ) {
        if (type == inheritanceInfo.getRootType()) {
            return resolvedMode == TypeMatchMode.EXACT;
        }
        if (inheritanceInfo.getStrategy() == InheritanceType.SINGLE_TABLE) {
            return true;
        }
        if (physicalUpdateType() != type) {
            return true;
        }
        if (resolvedMode == TypeMatchMode.POLYMORPHIC) {
            return false;
        }
        return inheritanceInfo.getConcreteTypes(type).size() > 1;
    }

    private Collection<ImmutableType> matchedTypes(
            InheritanceInfo inheritanceInfo,
            ImmutableType type,
            TypeMatchMode resolvedMode
    ) {
        if (resolvedMode == TypeMatchMode.EXACT) {
            return Collections.singleton(type);
        }
        Collection<ImmutableType> types = inheritanceInfo.getConcreteTypes(type);
        if (types.isEmpty()) {
            throw new ExecutionException(
                    "Cannot update inheritance entity type \"" +
                            type +
                            "\" polymorphically because it has no instantiable type"
            );
        }
        return types;
    }

    @Override
    public Integer execute(Connection con) {
        return getSqlClient()
                .getConnectionManager()
                .execute(con, this::executeImpl);
    }

    @Override
    public <R> SelectionExecutable<R> returning(Selection<R> selection) {
        return returning(Collections.singletonList(selection), null);
    }

    @Override
    public <T1, T2> SelectionExecutable<Tuple2<T1, T2>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2
    ) {
        return returning(Arrays.asList(selection1, selection2), null);
    }

    @Override
    public <T1, T2, T3> SelectionExecutable<Tuple3<T1, T2, T3>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3), null);
    }

    @Override
    public <T1, T2, T3, T4> SelectionExecutable<Tuple4<T1, T2, T3, T4>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4), null);
    }

    @Override
    public <T1, T2, T3, T4, T5> SelectionExecutable<Tuple5<T1, T2, T3, T4, T5>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4, selection5), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> SelectionExecutable<Tuple6<T1, T2, T3, T4, T5, T6>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4, selection5, selection6), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> SelectionExecutable<Tuple7<T1, T2, T3, T4, T5, T6, T7>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4, selection5, selection6, selection7), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> SelectionExecutable<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4, selection5, selection6, selection7, selection8), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> SelectionExecutable<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> returning(
            Selection<T1> selection1,
            Selection<T2> selection2,
            Selection<T3> selection3,
            Selection<T4> selection4,
            Selection<T5> selection5,
            Selection<T6> selection6,
            Selection<T7> selection7,
            Selection<T8> selection8,
            Selection<T9> selection9
    ) {
        return returning(Arrays.asList(selection1, selection2, selection3, selection4, selection5, selection6, selection7, selection8, selection9), null);
    }

    @Override
    public <R> SelectionExecutable<R> returning(TupleMapper<R> mapper) {
        return returning(mapper.getSelections(), mapper);
    }

    private <R> SelectionExecutable<R> returning(
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator
    ) {
        validateMutable();
        if (selections.isEmpty()) {
            throw new IllegalArgumentException("The update-returning selection list cannot be empty");
        }
        return new UpdateSelectionExecutable<>(selections, tupleCreator, jdbcOptions);
    }

    private int executeImpl(Connection con) {

        if (assignmentMap.isEmpty()) {
            return 0;
        }

        if (getSqlClient().isTargetTransferable()) {
            Executor.validateMutationConnection(con);
        }

        SqlBuilder builder = createFilteredBuilder();

        if (!triggerIgnored && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            return executeWithTrigger(builder, con);
        }

        renderTo(builder);
        return executeUpdateSql(builder, con);
    }

    private <R> List<R> executeReturningImpl(
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions
    ) {

        if (assignmentMap.isEmpty()) {
            return Collections.emptyList();
        }

        Dialect dialect = getSqlClient().getDialect();
        if (!dialect.isUpdateReturningSupported()) {
            throw new ExecutionException(
                    "The update-returning statement is not supported by \"" +
                            dialect.getClass().getName() +
                            "\""
            );
        }

        if (getSqlClient().isTargetTransferable()) {
            Executor.validateMutationConnection(con);
        }

        SqlBuilder builder = createFilteredBuilder();

        if (!triggerIgnored && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            return executeReturningWithTrigger(builder, con, selections, tupleCreator, jdbcOptions);
        }

        renderReturningTo(builder, null, selections);
        return executeReturningSql(builder, con, selections, tupleCreator, jdbcOptions);
    }

    private <R> Stream<R> streamReturningImpl(
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions
    ) {

        if (assignmentMap.isEmpty()) {
            return Stream.empty();
        }

        Dialect dialect = getSqlClient().getDialect();
        if (!dialect.isUpdateReturningSupported()) {
            throw new ExecutionException(
                    "The update-returning statement is not supported by \"" +
                            dialect.getClass().getName() +
                            "\""
            );
        }

        SqlBuilder builder = createFilteredBuilder();

        if (!triggerIgnored && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            return streamReturningWithTrigger(builder, con, selections, tupleCreator, jdbcOptions);
        }

        renderReturningTo(builder, null, selections);
        return streamReturningSql(builder, con, selections, tupleCreator, jdbcOptions, null);
    }

    private <R> List<R> executeReturningWithTrigger(
            SqlBuilder builder,
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions
    ) {
        Map<Object, ImmutableSpi> oldRowMap = selectRowsForTrigger(builder, con);
        if (oldRowMap.isEmpty()) {
            return Collections.emptyList();
        }

        builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderReturningTo(builder, oldRowMap.keySet(), selections);
        List<R> rows = executeReturningSql(builder, con, selections, tupleCreator, jdbcOptions);
        if (!rows.isEmpty()) {
            submitTrigger(con, oldRowMap);
        }
        return rows;
    }

    private <R> Stream<R> streamReturningWithTrigger(
            SqlBuilder builder,
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions
    ) {
        ConnectionManager.ConnectionScope scope = getSqlClient().getConnectionManager().open(con);
        Connection actualCon = scope.connection();
        try {
            if (getSqlClient().isTargetTransferable()) {
                Executor.validateMutationConnection(actualCon);
            }
            Map<Object, ImmutableSpi> oldRowMap = selectRowsForTrigger(builder, actualCon);
            if (oldRowMap.isEmpty()) {
                scope.close();
                return Stream.empty();
            }

            builder = new SqlBuilder(new AstContext(getSqlClient()));
            renderReturningTo(builder, oldRowMap.keySet(), selections);
            return streamReturningSql(
                    builder,
                    actualCon,
                    existingScopeConnectionManager(scope),
                    selections,
                    tupleCreator,
                    jdbcOptions,
                    () -> submitTrigger(actualCon, oldRowMap),
                    null
            );
        } catch (RuntimeException | Error ex) {
            scope.close();
            throw ex;
        }
    }

    private <R> List<R> executeReturningSql(
            SqlBuilder builder,
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions
    ) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.select(
                getSqlClient(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                selections,
                tupleCreator,
                getPurpose(),
                jdbcOptions
        );
    }

    private <R> Stream<R> streamReturningSql(
            SqlBuilder builder,
            Connection con,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions,
            @Nullable Runnable beforeClose
    ) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.stream(
                getSqlClient(),
                getSqlClient().getConnectionManager(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                selections,
                tupleCreator,
                getPurpose(),
                jdbcOptions,
                beforeClose,
                getSqlClient().isTargetTransferable() ? Executor::validateMutationConnection : null
        );
    }

    private <R> Stream<R> streamReturningSql(
            SqlBuilder builder,
            Connection con,
            ConnectionManager connectionManager,
            List<Selection<?>> selections,
            TupleCreator<R> tupleCreator,
            JdbcOptions jdbcOptions,
            @Nullable Runnable beforeClose,
            @Nullable java.util.function.Consumer<Connection> connectionValidator
    ) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.stream(
                getSqlClient(),
                connectionManager,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                selections,
                tupleCreator,
                getPurpose(),
                jdbcOptions,
                beforeClose,
                connectionValidator
        );
    }

    private ConnectionManager existingScopeConnectionManager(ConnectionManager.ConnectionScope scope) {
        return new ConnectionManager() {
            @Override
            public <R> R execute(@Nullable Connection con, java.util.function.Function<Connection, R> block) {
                return block.apply(scope.connection());
            }

            @Override
            public ConnectionManager.ConnectionScope open(@Nullable Connection con) {
                return scope;
            }
        };
    }

    private SqlBuilder createFilteredBuilder() {
        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
        updateQuery.applyVirtualPredicates(builder.getAstContext());
        updateQuery.applyGlobalFilters(builder.getAstContext(), FilterLevel.DEFAULT, null);
        return builder;
    }

    private int executeUpdateSql(SqlBuilder builder, Connection con) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return getSqlClient()
                .getExecutor()
                .execute(
                        new Executor.Args<>(
                                getSqlClient(),
                                con,
                                sqlResult.get_1(),
                                sqlResult.get_2(),
                                sqlResult.get_3(),
                                getPurpose(),
                                null,
                                null,
                                (stmt, args) -> stmt.executeUpdate()
                        )
                );
    }

    private int executeWithTrigger(SqlBuilder builder, Connection con) {

        Map<Object, ImmutableSpi> oldRowMap = selectRowsForTrigger(builder, con);
        if (oldRowMap.isEmpty()) {
            return 0;
        }

        builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderTo(builder, oldRowMap.keySet());
        int affectRowCount = executeUpdateSql(builder, con);
        if (affectRowCount == 0) {
            return 0;
        }

        submitTrigger(con, oldRowMap);
        return affectRowCount;
    }

    private Map<Object, ImmutableSpi> selectRowsForTrigger(SqlBuilder builder, Connection con) {
        renderAsSelect(builder, null);
        List<ImmutableSpi> rows = selectRows(builder, con);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        PropId idPropId = this.<Table<?>>getTable().getImmutableType().getIdProp().getId();
        Map<Object, ImmutableSpi> rowMap = new HashMap<>((rows.size() * 4 + 2) / 3);
        for (ImmutableSpi row : rows) {
            rowMap.put(row.__get(idPropId), row);
        }
        return rowMap;
    }

    private void submitTrigger(Connection con, Map<Object, ImmutableSpi> oldRowMap) {
        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderAsSelect(builder, oldRowMap.keySet());
        List<ImmutableSpi> changedRows = selectRows(builder, con);
        PropId idPropId = this.<Table<?>>getTable().getImmutableType().getIdProp().getId();
        MutationTrigger trigger = new MutationTrigger();
        for (ImmutableSpi changedRow : changedRows) {
            ImmutableSpi row = oldRowMap.get(changedRow.__get(idPropId));
            if (!row.__equals(changedRow, true)) {
                trigger.modifyEntityTable(row, changedRow);
            }
        }
        trigger.submit(getSqlClient(), con);
    }

    private List<ImmutableSpi> selectRows(SqlBuilder builder, Connection con) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return Selectors.select(
                getSqlClient(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Collections.singletonList(this.getTable()),
                null,
                ExecutionPurpose.UPDATE
        );
    }

    public void accept(@NotNull AstVisitor visitor) {
        accept(visitor, true);
    }

    public void renderTo(@NotNull SqlBuilder builder) {
        renderTo(builder, null);
    }

    private void renderReturningTo(
            SqlBuilder builder,
            Collection<Object> ids,
            List<Selection<?>> selections
    ) {
        getSqlClient().getDialect().updateReturning(
                new UpdateReturningContextImpl(builder, ids, selections)
        );
    }

    void shareRootAliasWith(TableLikeImplementor<?> source) {
        this.aliasSource = source;
    }

    @Override
    public TableImplementor<?> getTableLikeImplementor() {
        return (TableImplementor<?>) updateQuery.getTableLikeImplementor();
    }

    AbstractMutableStatementImpl wrappedQuery() {
        return updateQuery;
    }

    private ImmutableType physicalUpdateType() {
        ImmutableType stageType = primaryAssignmentStageType;
        return stageType != null ? stageType : getTableLikeImplementor().getImmutableType();
    }

    private boolean isJoinedTypeBranchUpdate() {
        TableImplementor<?> table = getTableLikeImplementor();
        return table.isJoinedTypeBranchRoot() && physicalUpdateType() == table.getImmutableType();
    }

    private void accept(@NotNull AstVisitor visitor, boolean visitAssignments) {
        AstContext astContext = visitor.getAstContext();
        freeze(astContext);
        astContext.pushStatement(updateQuery);
        visitor.visitStatement(this);
        try {
            if (visitAssignments) {
                for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
                    ((Ast) e.getKey().expr).accept(visitor);
                    ((Ast) e.getValue()).accept(visitor);
                }
            }
            for (Predicate predicate : updateQuery.unfrozenPredicates()) {
                ((Ast) predicate).accept(visitor);
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void addAlias(SqlBuilder builder) {
        if (getSqlClient().getDialect().isUpdateNeedsAsKeyword()) {
            builder.sql(" as ");
        } else {
            builder.sql(" ");
        }
        builder.sql(MutationRender.alias(builder, getTableLikeImplementor()));
    }

    private void renderTo(@NotNull SqlBuilder builder, Collection<Object> ids) {
        renderTo(builder, ids, null);
    }

    private void updateSetters(SqlBuilder builder,
                               boolean joinedTypeStageJoinRequired,
                               ImmutableType physicalType,
                               Map<ImmutableType, String> joinedTypeStageAliasMap){
        builder.enter(SqlBuilder.ScopeType.SET);
        renderAssignments(builder, joinedTypeStageJoinRequired, physicalType, joinedTypeStageAliasMap);
        builder.leave();
    }

    private void renderTo(
            @NotNull SqlBuilder builder,
            Collection<Object> ids,
            @Nullable List<Selection<?>> returningSelections
    ) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(updateQuery);
        boolean joinedTypeBranchUpdatePushed = false;
        try {
            TableImplementor<?> table = getTableLikeImplementor();
            ImmutableType physicalType = physicalUpdateType();
            Dialect dialect = getSqlClient().getDialect();
            UpdateJoin updateJoin = dialect.getUpdateJoin();
            VisitorImpl visitor = new VisitorImpl(builder.getAstContext(), updateJoin != null ? dialect : null);
            this.accept(visitor);
            TableUsages tableUsages = visitor.toTableUsages();
            tableUsages.applyUsedStatesTo(astContext);
            tableUsages.allocateAndBindAliases(astContext);
            Collection<ImmutableType> joinedTypeStageTypes = joinedTypeStageTypes(visitor);
            Map<ImmutableType, String> joinedTypeStageAliasMap =
                    joinedTypeStageAliasMap(builder, joinedTypeStageTypes, physicalType);
            Map<ImmutableType, String> idSubQueryStageAliasMap = Collections.emptyMap();
            boolean usedChild = MutationJoinRenderSupport.hasUsedChild(table, astContext);
            boolean joinedTypeStageJoinRequired = !joinedTypeStageAliasMap.isEmpty();
            boolean idSubQueryRequired =
                    ids == null &&
                            updateJoin == null &&
                            (joinedTypeStageJoinRequired || usedChild);
            ImmutableType rootType = table.getImmutableType().getInheritanceInfo() != null ?
                    table.getImmutableType().getInheritanceInfo().getRootType() :
                    null;
            if (idSubQueryRequired) {
                if (!dialect.isTableOfSubQueryMutable()) {
                    throw new ExecutionException(
                            "Table joins for update statement is forbidden by the current dialect, " +
                                    "and the current dialect does not support using the updated table in a subquery. " +
                                    "Cannot render portable id-subquery update for \"" +
                                    table.getImmutableType() +
                                    "\""
                    );
                }
                if (assignmentSourceRequiresExtraTable()) {
                    throw new ExecutionException(
                            "Table joins for update statement is forbidden by the current dialect, " +
                                    "but an assignment value of update statement for \"" +
                                    table.getImmutableType() +
                                    "\" requires another table. Portable id-subquery update can only move " +
                                    "predicate joins into the id subquery, not assignment value joins."
                    );
                }
                ImmutableType subQueryBaseType = rootType != null ? rootType : table.getImmutableType();
                idSubQueryStageAliasMap = joinedTypeStageAliasMap(builder, joinedTypeStageTypes, subQueryBaseType);
                joinedTypeStageAliasMap = joinedTypeStageAliasMap(builder, assignmentStageTypes, physicalType);
                joinedTypeStageJoinRequired = !joinedTypeStageAliasMap.isEmpty();
            }
            if (joinedTypeStageJoinRequired &&
                    updateJoin != null &&
                    updateJoin.getFrom() == UpdateJoin.From.AS_ROOT) {
                throw new ExecutionException(
                        "The current dialect renders update joins from the root table, " +
                                "but joined inheritance update for \"" +
                                table.getImmutableType() +
                                "\" requires separate inheritance stage aliases"
                );
            }
            if (aliasSource != null) {
                astContext.getTableAliasScope().bindAlias(
                        aliasSource.realTable(astContext),
                        getTableLikeImplementor().realTable(astContext)
                );
            }
            if (!idSubQueryRequired &&
                    table.isJoinedTypeBranchRoot() &&
                    (physicalType != table.getImmutableType() ||
                            isJoinedTypeBranchUpdate() ||
                            joinedTypeStageJoinRequired)) {
                astContext.pushJoinedTypeBranchUpdate(
                        table,
                        physicalType,
                        rootType != null ? joinedTypeStageAliasMap.get(rootType) : null,
                        joinedTypeStageAliasMap
                );
                joinedTypeBranchUpdatePushed = true;
            }
            builder.sql("update");
            if (dialect.isUpdateAliasRequired()) {
                // SQL Server: update alias SET ... from TABLE alias WHERE ...
                // Alias comes right after UPDATE keyword, before SET clause
                addAlias(builder);
                updateSetters(builder, joinedTypeStageJoinRequired, physicalType, joinedTypeStageAliasMap);
                builder.sql(" from");
            }
            builder.sql(" ");
            builder.sql(physicalType.getTableName(getSqlClient().getMetadataStrategy()));

            addAlias(builder);

            if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
                renderJoinedTypeStageJoins(builder, physicalType, joinedTypeStageAliasMap);
                for (RealTable child : table.realTable(astContext)) {
                    child.renderTo(builder, false);
                }
            }

            if(!dialect.isUpdateAliasRequired()) {
                updateSetters(builder, joinedTypeStageJoinRequired, physicalType, joinedTypeStageAliasMap);
            }

            renderTables(builder, physicalType, joinedTypeStageAliasMap);
            renderDeeperJoins(builder);

            renderWhereClause(
                    builder,
                    true,
                    ids,
                    physicalType,
                    joinedTypeStageAliasMap,
                    idSubQueryStageAliasMap,
                    idSubQueryRequired
            );

            if (returningSelections != null) {
                builder.sql(" returning ");
                renderReturningSelections(builder, returningSelections, physicalType, true);
            }

        } finally {
            if (joinedTypeBranchUpdatePushed) {
                astContext.popJoinedTypeBranchUpdate();
            }
            astContext.popStatement();
        }
    }

    private void renderAsSelect(SqlBuilder builder, Collection<Object> ids) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(updateQuery);
        try {
            VisitorImpl visitor = new VisitorImpl(builder.getAstContext(), null);
            accept(visitor, false);
            TableUsages tableUsages = visitor.toTableUsages();
            tableUsages.applyUsedStatesTo(astContext);
            tableUsages.allocateAndBindAliases(astContext);
            TableImplementor<?> table = getTableLikeImplementor();
            MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
            builder.enter(SqlBuilder.ScopeType.SELECT);
            for (ImmutableProp prop : table.getImmutableType().getSelectableProps().values()) {
                builder.separator().definition(
                        MutationRender.alias(builder, table),
                        prop.getStorage(strategy),
                        null
                );
            }
            builder.leave();
            if (ids != null) {
                builder
                        .from()
                        .sql(table.getImmutableType().getTableName(strategy));

                addAlias(builder);

                builder.enter(SqlBuilder.ScopeType.WHERE);
                NativePredicates.renderPredicates(
                        false,
                        MutationRender.alias(builder, table),
                        table.getImmutableType().getIdProp().getStorage(strategy),
                        ids,
                        builder
                );
                builder.leave();
            } else {
                table.renderTo(builder);
                renderWhereClause(
                        builder,
                        false,
                        null,
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void renderAssignments(
            SqlBuilder builder,
            boolean joinedTypeStageJoinRequired,
            ImmutableType physicalType,
            Map<ImmutableType, String> joinedTypeStageAliasMap
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean withTargetPrefix =
                updateJoin != null &&
                        updateJoin.isJoinedTableUpdatable() &&
                        (MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext()) ||
                                joinedTypeStageJoinRequired);
        for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
            builder.separator();
            renderTarget(builder, e.getKey(), withTargetPrefix, physicalType, joinedTypeStageAliasMap);
            builder.sql(" = ");
            renderAssignmentSource(e.getValue(), e.getKey().expr.getDeepestProp(), builder);
        }
    }

    private void renderAssignmentSource(Expression<?> source, ImmutableProp prop, SqlBuilder builder) {
        if (source instanceof LiteralExpressionImplementor<?>) {
            Object value = ((LiteralExpressionImplementor<?>) source).getValue();
            builder.variable(Variables.process(value, prop, builder.getAstContext().getSqlClient()));
        } else {
            ((Ast) source).renderTo(builder);
        }
    }

    private void renderTarget(
            SqlBuilder builder,
            Target target,
            boolean withPrefix,
            ImmutableType physicalType,
            Map<ImmutableType, String> joinedTypeStageAliasMap
    ) {
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        ColumnDefinition definition;
        if (target.prop.isEmbedded(EmbeddedLevel.REFERENCE)) {
            String name = target.expr.getPartial(strategy).name(0);
            MultipleJoinColumns joinColumns = target.prop.getStorage(strategy);
            definition = new SingleColumn(
                    joinColumns.name(joinColumns.referencedIndex(name)),
                    joinColumns.isForeignKey(),
                    null,
                    null
            );
        } else if (target.prop.isReference(TargetLevel.ENTITY)) {
            definition = target.prop.getStorage(strategy);
        } else {
            EmbeddedColumns.Partial partial = target.expr.getPartial(strategy);
            definition = partial != null ? partial : target.prop.getStorage(strategy);
        }
        builder.definition(
                withPrefix ? assignmentTargetAlias(builder, target, physicalType, joinedTypeStageAliasMap) : null,
                definition
        );
    }

    private String assignmentTargetAlias(
            SqlBuilder builder,
            Target target,
            ImmutableType physicalType,
            Map<ImmutableType, String> joinedTypeStageAliasMap
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        if (table.isJoinedTypeBranchRoot()) {
            ImmutableType stageType = table.getImmutableType()
                    .getInheritanceInfo()
                    .getTableTypeForProp(target.prop, table.getImmutableType());
            if (stageType != null && stageType != physicalType) {
                return joinedTypeStageAliasMap.get(stageType);
            }
        }
        return MutationRender.alias(builder, TableProxies.resolve(target.table, builder.getAstContext()));
    }

    private void renderTables(
            SqlBuilder builder,
            @Nullable ImmutableType physicalType,
            Map<ImmutableType, String> joinedTypeStageAliasMap
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        boolean joinedTypeStageJoinRequired = !joinedTypeStageAliasMap.isEmpty();
        boolean usedChild = MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext());
        if (joinedTypeStageJoinRequired || usedChild) {
            UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
            if (updateJoin == null) {
                return;
            }
            switch (updateJoin.getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.from().enter(SqlBuilder.ScopeType.COMMA);
                    renderJoinedTypeStageFroms(builder, physicalType, joinedTypeStageAliasMap);
                    MutationJoinRenderSupport.renderUsedJoinsAsFrom(builder, table);
                    builder.leave();
            }
        }
    }

    private void renderDeeperJoins(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        if (updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext())
        ) {
            MutationJoinRenderSupport.renderDeeperJoinsAsFrom(builder, table);
        }
    }

    private void renderWhereClause(
            SqlBuilder builder,
            boolean forUpdate,
            Collection<Object> ids,
            @Nullable ImmutableType physicalType,
            Map<ImmutableType, String> joinedTypeStageAliasMap,
            Map<ImmutableType, String> idSubQueryStageAliasMap,
            boolean idSubQueryRequired
    ) {

        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();

        boolean hasJoinedTypeStageCondition =
                forUpdate &&
                        updateJoin != null &&
                        updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                        !joinedTypeStageAliasMap.isEmpty();

        boolean hasTableCondition =
                forUpdate &&
                        updateJoin != null &&
                        updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                        MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext());

        if (!hasJoinedTypeStageCondition &&
                !hasTableCondition &&
                ids == null &&
                !idSubQueryRequired &&
                !updateQuery.unfrozenPredicates().iterator().hasNext()) {
            return;
        }

        builder.enter(SqlBuilder.ScopeType.WHERE);
        if (ids != null) {
            NativePredicates.renderPredicates(
                    false,
                    MutationRender.alias(builder, table),
                    table.getImmutableType().getIdProp().getStorage(getSqlClient().getMetadataStrategy()),
                    ids,
                    builder
            );
        }

        if (idSubQueryRequired) {
            builder.separator();
            if (physicalType == null) {
                throw new AssertionError("Internal bug: physical update type must be specified for id subquery");
            }
            MutationQuerySupport.renderIdInSubQuery(
                    builder,
                    updateQuery,
                    table,
                    physicalType,
                    idSubQueryStageAliasMap
            );
        }

        if (hasJoinedTypeStageCondition) {
            renderJoinedTypeStageConditions(builder, physicalType, joinedTypeStageAliasMap);
        }

        if (hasTableCondition) {
            MutationJoinRenderSupport.renderUsedJoinConditions(builder, table);
        }

        if (ids == null && !idSubQueryRequired) {
            Predicate predicate = updateQuery.getPredicate(builder.getAstContext());
            if (predicate != null) {
                builder.separator();
                ((Ast) predicate).renderTo(builder);
            }
        }

        builder.leave();
    }

    private boolean assignmentSourceRequiresExtraTable() {
        AstContext astContext = new AstContext(getSqlClient());
        TableImplementor<?> table = getTableLikeImplementor();
        VisitorImpl visitor = new VisitorImpl(astContext, null);
        astContext.pushStatement(updateQuery);
        try {
            for (Expression<?> expression : assignmentMap.values()) {
                ((Ast) expression).accept(visitor);
            }
        } finally {
            astContext.popStatement();
        }
        return !visitor.joinedTypeStageTypes().isEmpty() ||
                MutationJoinRenderSupport.hasUsedChild(table, astContext);
    }

    private Map<ImmutableType, String> joinedTypeStageAliasMap(
            SqlBuilder builder,
            Collection<ImmutableType> stageTypes,
            ImmutableType physicalType
    ) {
        if (stageTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        TableImplementor<?> table = getTableLikeImplementor();
        Map<ImmutableType, String> aliasMap = new LinkedHashMap<>();
        for (ImmutableType stageType : stageTypes) {
            if (stageType != physicalType) {
                aliasMap.put(stageType, joinedTypeStageAlias(builder, table, stageType, physicalType));
            }
        }
        return aliasMap;
    }

    private Collection<ImmutableType> joinedTypeStageTypes(VisitorImpl visitor) {
        if (assignmentStageTypes.isEmpty()) {
            return visitor.joinedTypeStageTypes();
        }
        Set<ImmutableType> stageTypes = new LinkedHashSet<>(assignmentStageTypes);
        stageTypes.addAll(visitor.joinedTypeStageTypes());
        return stageTypes;
    }

    private String joinedTypeStageAlias(
            SqlBuilder builder,
            TableImplementor<?> table,
            ImmutableType stageType,
            ImmutableType physicalType
    ) {
        return MutationJoinRenderSupport.joinedTypeStageAlias(builder, table, stageType, physicalType);
    }

    private void renderJoinedTypeStageJoins(
            SqlBuilder builder,
            ImmutableType physicalType,
            Map<ImmutableType, String> aliasMap
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        for (Map.Entry<ImmutableType, String> e : aliasMap.entrySet()) {
            MutationJoinRenderSupport.renderJoinedTypeStageJoin(
                    builder,
                    table,
                    physicalType,
                    e.getKey(),
                    e.getValue()
            );
        }
    }

    private void renderJoinedTypeStageFroms(
            SqlBuilder builder,
            @Nullable ImmutableType physicalType,
            Map<ImmutableType, String> aliasMap
    ) {
        for (Map.Entry<ImmutableType, String> e : aliasMap.entrySet()) {
            builder.separator();
            MutationJoinRenderSupport.renderJoinedTypeStageFrom(builder, e.getKey(), e.getValue());
        }
    }

    private void renderJoinedTypeStageConditions(
            SqlBuilder builder,
            @Nullable ImmutableType physicalType,
            Map<ImmutableType, String> aliasMap
    ) {
        if (physicalType == null) {
            return;
        }
        TableImplementor<?> table = getTableLikeImplementor();
        for (Map.Entry<ImmutableType, String> e : aliasMap.entrySet()) {
            builder.separator();
            MutationJoinRenderSupport.renderJoinedTypeStageCondition(
                    builder,
                    table,
                    physicalType,
                    e.getKey(),
                    e.getValue()
            );
        }
    }

    private void renderReturningSelections(
            SqlBuilder builder,
            List<Selection<?>> selections,
            ImmutableType physicalType,
            boolean withPrefix
    ) {
        builder.enter(SqlBuilder.ScopeType.COMMA);
        try {
            for (Selection<?> selection : selections) {
                builder.separator();
                PropExpressionImplementor<?> propExpression =
                        returningPropExpression(builder.getAstContext(), selection, physicalType);
                if (withPrefix) {
                    propExpression.renderTo(builder, true);
                } else {
                    renderReturningColumnWithoutPrefix(builder, propExpression);
                }
            }
        } finally {
            builder.leave();
        }
    }

    private PropExpressionImplementor<?> returningPropExpression(
            AstContext astContext,
            Selection<?> selection,
            ImmutableType physicalType
    ) {
        Ast ast = Ast.from(selection, astContext);
        if (!(ast instanceof PropExpressionImplementor<?>)) {
            throw new IllegalArgumentException(
                    "The update-returning selection \"" +
                            selection +
                            "\" is not supported. Only column property selections of the updated table are supported."
            );
        }
        PropExpressionImplementor<?> propExpression = (PropExpressionImplementor<?>) ast;
        Table<?> selectionTable = propExpression.getTable();
        if (selectionTable != getTable() && selectionTable != getTableLikeImplementor()) {
            throw new IllegalArgumentException(
                    "The update-returning selection \"" +
                            selection +
                            "\" is not supported because it does not belong to the updated table \"" +
                            getTableLikeImplementor().getImmutableType() +
                            "\""
            );
        }
        ImmutableProp prop = propExpression.getProp();
        if (!prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The update-returning selection \"" +
                            selection +
                            "\" is not supported because property \"" +
                            prop +
                            "\" is not mapped by physical columns"
            );
        }
        validateReturningStage(prop, physicalType);
        return propExpression;
    }

    private void validateReturningStage(ImmutableProp prop, ImmutableType physicalType) {
        TableImplementor<?> table = getTableLikeImplementor();
        ImmutableType updateType = table.getImmutableType();
        InheritanceInfo inheritanceInfo = updateType.getInheritanceInfo();
        if (inheritanceInfo == null || inheritanceInfo.getStrategy() != InheritanceType.JOINED) {
            return;
        }
        if (assignmentStageTypes.size() > 1) {
            throw new IllegalArgumentException(
                    "The update-returning statement for joined inheritance type \"" +
                            updateType +
                            "\" cannot be used when assignments target multiple physical tables"
            );
        }
        ImmutableProp originalProp = prop.toOriginal();
        ImmutableType stageType = originalProp.isId() ?
                physicalType :
                inheritanceInfo.getTableTypeForProp(originalProp, updateType);
        if (stageType != physicalType) {
            MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
            throw new IllegalArgumentException(
                    "The update-returning property \"" +
                            prop +
                            "\" must belong to the same physical table as the update target. " +
                            "The property belongs to table \"" +
                            (stageType != null ? stageType.getTableName(strategy) : originalProp.getDeclaringType().getTableName(strategy)) +
                            "\" but the update target table is \"" +
                            physicalType.getTableName(strategy) +
                            "\""
            );
        }
    }

    private void renderReturningColumnWithoutPrefix(
            SqlBuilder builder,
            PropExpressionImplementor<?> propExpression
    ) {
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        EmbeddedColumns.Partial partial = propExpression.getPartial(strategy);
        TableSelection tableSelection = TableProxies.resolve(
                propExpression.getTable(),
                builder.getAstContext()
        );
        tableSelection.renderSelection(
                propExpression.getProp(),
                propExpression.isRawId(),
                builder,
                propExpression.getPath() != null ? partial : null,
                false
        );
    }

    private static class Target {

        final Table<?> table;

        final ImmutableProp prop;

        final PropExpressionImplementor<?> expr;

        private Target(Table<?> table, ImmutableProp prop, PropExpression<?> expr) {
            this.table = table;
            this.prop = prop;
            this.expr = (PropExpressionImplementor<?>) expr;
        }

        static Target of(PropExpression<?> expr, MetadataStrategy strategy) {
            PropExpressionImplementor<?> implementor = (PropExpressionImplementor<?>) expr;
            EmbeddedColumns.Partial partial = implementor.getPartial(strategy);
            if (partial != null && partial.isEmbedded()) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                implementor +
                                "\" is embedded, it cannot be used as the assignment target of update statement"
                );
            }
            Table<?> targetTable = implementor.getTable();
            Table<?> parent;
            ImmutableProp prop;
            if (targetTable instanceof TableImplementor<?>) {
                parent = ((TableImplementor<?>) targetTable).getParent();
                prop = ((TableImplementor<?>) targetTable).getJoinProp();
            } else {
                parent = ((TableProxy<?>) targetTable).__parent();
                prop = ((TableProxy<?>) targetTable).__prop();
            }
            if (parent != null && prop != null && implementor.getProp().isId()) {
                return new Target(parent, prop, expr);
            } else {
                return new Target(targetTable, implementor.getProp(), expr);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;
            return expr.equals(target.expr);
        }

        @Override
        public int hashCode() {
            return expr.hashCode();
        }
    }

    private class UpdateSelectionExecutable<R> implements SelectionExecutable<R> {

        private final List<Selection<?>> selections;

        private final TupleCreator<R> tupleCreator;

        private final JdbcOptions jdbcOptions;

        private UpdateSelectionExecutable(
                List<Selection<?>> selections,
                TupleCreator<R> tupleCreator,
                JdbcOptions jdbcOptions
        ) {
            this.selections = selections;
            this.tupleCreator = tupleCreator;
            this.jdbcOptions = jdbcOptions;
        }

        @Override
        public List<R> execute(Connection con) {
            return getSqlClient()
                    .getConnectionManager()
                    .execute(con, it -> executeReturningImpl(it, selections, tupleCreator, jdbcOptions));
        }

        @Override
        public Stream<R> stream(Connection con) {
            return streamReturningImpl(con, selections, tupleCreator, jdbcOptions);
        }
    }

    private class UpdateReturningContextImpl implements Dialect.UpdateReturningContext {

        private final SqlBuilder builder;

        private final Collection<Object> ids;

        private final List<Selection<?>> selections;

        private UpdateReturningContextImpl(
                SqlBuilder builder,
                Collection<Object> ids,
                List<Selection<?>> selections
        ) {
            this.builder = builder;
            this.ids = ids;
            this.selections = selections;
        }

        @Override
        public Dialect.UpdateReturningContext sql(String sql) {
            builder.sql(sql);
            return this;
        }

        @Override
        public Dialect.UpdateReturningContext appendUpdateStatement() {
            renderTo(builder, ids);
            return this;
        }

        @Override
        public Dialect.UpdateReturningContext appendUpdateStatementWithReturning() {
            renderTo(builder, ids, selections);
            return this;
        }

        @Override
        public Dialect.UpdateReturningContext appendReturningColumns() {
            AstContext astContext = builder.getAstContext();
            astContext.pushStatement(updateQuery);
            try {
                renderReturningSelections(builder, selections, physicalUpdateType(), false);
            } finally {
                astContext.popStatement();
            }
            return this;
        }
    }

    private class VisitorImpl extends TableUsageCollector {

        private final Dialect dialect;

        private final Set<ImmutableType> joinedTypeStageTypes = new LinkedHashSet<>();

        public VisitorImpl(AstContext astContext, Dialect dialect) {
            super(astContext);
            this.dialect = dialect;
        }

        @Override
        public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
            super.visitTableReference(table, prop, rawId);
            collectJoinedTypeStageRequirement(table, prop);
            if (dialect != null) {
                validateTable(table);
            }
        }

        Collection<ImmutableType> joinedTypeStageTypes() {
            return joinedTypeStageTypes;
        }

        private void collectJoinedTypeStageRequirement(RealTable table, @Nullable ImmutableProp prop) {
            TableImplementor<?> updateTable = getTableLikeImplementor();
            if (!updateTable.isJoinedTypeBranchRoot()) {
                return;
            }
            ImmutableType updateType = updateTable.getImmutableType();
            ImmutableType physicalType = physicalUpdateType();
            if (table.getTableLikeImplementor() == updateTable) {
                collectJoinedTypeStage(prop, updateType, physicalType);
                return;
            }
            for (RealTable current = table; current.getParent() != null; current = current.getParent()) {
                if (current.getParent().getTableLikeImplementor() == updateTable &&
                        current.getTableLikeImplementor() instanceof TableImplementor<?>) {
                    ImmutableProp joinProp = ((TableImplementor<?>) current.getTableLikeImplementor()).getJoinProp();
                    collectJoinedTypeStage(joinProp, updateType, physicalType);
                    return;
                }
            }
        }

        private void collectJoinedTypeStage(
                @Nullable ImmutableProp prop,
                ImmutableType updateType,
                ImmutableType physicalType
        ) {
            if (prop == null || prop.isId() || prop.toOriginal().isId()) {
                return;
            }
            ImmutableType stageType = updateType.getInheritanceInfo().getTableTypeForProp(prop, updateType);
            if (stageType != null && stageType != physicalType) {
                joinedTypeStageTypes.add(stageType);
            }
        }

        private void validateTable(RealTable table) {
            if (table.getTableLikeImplementor().getStatement() != updateQuery) {
                return;
            }
            if (getTableUsedState(table) == TableUsedState.USED) {
                if (table.getParent() != null && dialect.getUpdateJoin() == null) {
                    throw new ExecutionException(
                            "Table joins for update statement is forbidden by the current dialect, " +
                                    "but there is a join '" +
                                    table.getTableLikeImplementor() +
                                    "'."
                    );
                }
                if (table.getParent() != null &&
                        table.getParent().getParent() == null &&
                        dialect.getUpdateJoin() != null &&
                        dialect.getUpdateJoin().getFrom() == UpdateJoin.From.AS_JOIN) {
                    Lazy<String> reason = new Lazy<>(() ->
                            "because current dialect '" +
                                    dialect.getClass().getName() +
                                    "' " +
                                    "indicates that the first level table joins in update statement " +
                                    "must be rendered as 'from' clause, " +
                                    "but there is a first level table join whose join type is outer: '" +
                                    table.getTableLikeImplementor() +
                                    "'."
                    );
                    TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
                    if (implementor instanceof TableImplementor<?>) {
                        TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                        if (tableImplementor.getJoinType() != JoinType.INNER) {
                            throw new ExecutionException(
                                    "The first level table joins cannot be outer join " + reason.get()
                            );
                        }
                        if (tableImplementor.getWeakJoinHandle() != null) {
                            throw new ExecutionException(
                                    "The first level table joins cannot be weak join " + reason.get()
                            );
                        }
                    }
                }
            }
            if (table.getParent() != null) {
                validateTable(table.getParent());
            }
        }
    }
}
