package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.meta.spi.ImmutableTypeImplementor;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.Associations;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableProxies;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.query.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.ast.query.selectable.ReturningSelectable;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.InsertFromSelectContext;
import org.babyfish.jimmer.sql.dialect.InsertFromSelectMode;
import org.babyfish.jimmer.sql.dialect.InsertFromSelectRenderer;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.SequenceIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Stream;

abstract class AbstractInsertFromSelectImpl<S extends BaseTable>
        extends AbstractMutableStatementImpl
        implements ReturningSelectable {

    enum Role {KEY, INSERT, MERGE}

    final S source;

    final MutableRootQueryImpl<BaseTable> sourceQuery;

    final TableLikeImplementor<?> sourceTable;

    final BaseTableSymbol sourceSymbol;

    final TypedBaseQueryImplementor<?> sourceAst;

    final StatementContext context = new StatementContext(ExecutionPurpose.UPDATE);

    final LinkedHashMap<Target, Assignment> assignments = new LinkedHashMap<>();

    final List<Predicate> updatePredicates = new ArrayList<>();

    @Nullable
    private Predicate discriminatorUpdatePredicate;

    private boolean implicitAssignmentsAdded;

    private boolean materializedIdRequired;

    @Nullable
    private Target implicitSequenceIdTarget;

    private boolean materializedPlan;

    private List<Expression<?>> materializedSourceExpressions = Collections.emptyList();

    private final TypedQueryImplementor analysisAst = new AnalysisAst();

    AbstractInsertFromSelectImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> target,
            S source
    ) {
        super(sqlClient, target);
        this.source = Objects.requireNonNull(source, "source cannot be null");
        BaseTable rawSource = BaseTableProxies.unwrap(source);
        if (!(rawSource instanceof BaseTableSymbol)) {
            throw new IllegalArgumentException("source must be a typed base-table symbol");
        }
        this.sourceSymbol = (BaseTableSymbol) rawSource;
        ConfigurableBaseQueryImpl<?> configurableSource = sourceSymbol.getQuery();
        MergedBaseQueryImpl<?> mergedSource = MergedBaseQueryImpl.from(configurableSource);
        this.sourceAst = mergedSource != null ? mergedSource : configurableSource;
        this.sourceQuery = new MutableRootQueryImpl<>(
                sqlClient,
                rawSource,
                ExecutionPurpose.UPDATE,
                FilterLevel.DEFAULT
        );
        this.sourceTable = sourceQuery.getTableLikeImplementor();
        validateTargetType();
    }

    AbstractInsertFromSelectImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType targetType,
            S source
    ) {
        super(sqlClient, targetType);
        this.source = Objects.requireNonNull(source, "source cannot be null");
        BaseTable rawSource = BaseTableProxies.unwrap(source);
        if (!(rawSource instanceof BaseTableSymbol)) {
            throw new IllegalArgumentException("source must be a typed base-table symbol");
        }
        this.sourceSymbol = (BaseTableSymbol) rawSource;
        ConfigurableBaseQueryImpl<?> configurableSource = sourceSymbol.getQuery();
        MergedBaseQueryImpl<?> mergedSource = MergedBaseQueryImpl.from(configurableSource);
        this.sourceAst = mergedSource != null ? mergedSource : configurableSource;
        this.sourceQuery = new MutableRootQueryImpl<>(
                sqlClient,
                rawSource,
                ExecutionPurpose.UPDATE,
                FilterLevel.DEFAULT
        );
        this.sourceTable = sourceQuery.getTableLikeImplementor();
        validateTargetType();
    }

    public TableImplementor<?> getTargetTableImplementor() {
        return (TableImplementor<?>) getTableLikeImplementor();
    }

    private void validateTargetType() {
        ImmutableType type = getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null && inheritanceInfo.getStrategy() == InheritanceType.JOINED) {
            throw new IllegalArgumentException(
                    "Insert/upsert from select does not support joined-inheritance target type \"" +
                            type + "\""
            );
        }
    }

    @Override
    public StatementContext getContext() {
        return context;
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
    }

    final <T> void addAssignment(
            PropExpression<T> targetExpression,
            Expression<T> insertSource,
            @Nullable Expression<T> updateExpression,
            Role role
    ) {
        validateMutable();
        Objects.requireNonNull(insertSource, "source expression cannot be null");
        Target target = Target.of(targetExpression, getSqlClient().getMetadataStrategy());
        validateTarget(target);
        Literals.bind(insertSource, targetExpression);
        validateAssignmentType(targetExpression, insertSource);
        if (updateExpression != null) {
            Literals.bind(updateExpression, targetExpression);
            validateAssignmentType(targetExpression, updateExpression);
        }
        Assignment assignment = new Assignment(target, insertSource, updateExpression, role);
        if (assignments.putIfAbsent(target, assignment) != null) {
            throw new IllegalStateException(
                    "The target property \"" + target.prop + "\" cannot be assigned more than once"
            );
        }
        validateNoOverlappingColumns(target);
    }

    private static void validateAssignmentType(
            PropExpression<?> target,
            Expression<?> source
    ) {
        Class<?> targetType = Classes.boxTypeOf(((ExpressionImplementor<?>) target).getType());
        Class<?> sourceType = Classes.boxTypeOf(((ExpressionImplementor<?>) source).getType());
        if (!targetType.isAssignableFrom(sourceType)) {
            throw new IllegalArgumentException(
                    "The source expression type \"" + sourceType.getName() +
                            "\" is incompatible with target expression type \"" +
                            targetType.getName() + "\""
            );
        }
    }

    private void validateTarget(Target target) {
        if (!AbstractTypedTable.__refEquals(getTable(), target.table)) {
            throw new IllegalArgumentException(
                    "The assignment target \"" + target.expr + "\" does not belong to the mutation target"
            );
        }
        if (!target.prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "The assignment target property \"" + target.prop + "\" is not backed by physical columns"
            );
        }
    }

    private void validateNoOverlappingColumns(Target target) {
        Set<String> newNames = target.columnNames(getSqlClient().getMetadataStrategy());
        for (Target oldTarget : assignments.keySet()) {
            if (oldTarget == target) {
                continue;
            }
            Set<String> names = oldTarget.columnNames(getSqlClient().getMetadataStrategy());
            for (String name : newNames) {
                if (names.contains(name)) {
                    assignments.remove(target);
                    throw new IllegalStateException(
                            "The physical target column \"" + name + "\" cannot be assigned more than once"
                    );
                }
            }
        }
    }

    final void addUpdatePredicates(Predicate... predicates) {
        validateMutable();
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                updatePredicates.add(predicate);
            }
        }
    }

    public Integer execute(Connection con) {
        return getSqlClient().getConnectionManager().execute(con, this::executeCount);
    }

    private int executeCount(Connection con) {
        SqlBuilder builder = prepareBuilder(null);
        if (materializedPlan) {
            return executeMaterialized(con, builder, null, null).affectedRowCount;
        }
        renderMutation(builder, null);
        Tuple3<String, List<Object>, List<Integer>> sql = builder.build();
        return getSqlClient().getExecutor().execute(
                new Executor.Args<>(
                        getSqlClient(),
                        con,
                        sql.get_1(),
                        sql.get_2(),
                        sql.get_3(),
                        ExecutionPurpose.command(QueryReason.NONE),
                        null,
                        null,
                        (stmt, args) -> stmt.executeUpdate()
                )
        );
    }

    private <R> List<R> executeReturning(
            Connection con,
            List<Selection<?>> selections,
            @Nullable TupleCreator<R> tupleCreator
    ) {
        SqlBuilder builder = prepareBuilder(selections);
        if (materializedPlan) {
            return executeMaterialized(con, builder, selections, tupleCreator).rows;
        }
        renderMutation(builder, selections);
        Tuple3<String, List<Object>, List<Integer>> sql = builder.build();
        return Selectors.select(
                getSqlClient(),
                con,
                sql.get_1(),
                sql.get_2(),
                sql.get_3(),
                selections,
                tupleCreator,
                ExecutionPurpose.command(QueryReason.NONE)
        );
    }

    private SqlBuilder prepareBuilder(@Nullable List<Selection<?>> returningSelections) {
        validateCommand(returningSelections);
        materializedPlan = isMaterializedExecutionRequired(returningSelections);
        if (materializedPlan && implicitSequenceIdTarget != null) {
            assignments.remove(implicitSequenceIdTarget);
            implicitSequenceIdTarget = null;
        }
        materializedSourceExpressions = materializedPlan ?
                collectMaterializedSourceExpressions() :
                Collections.emptyList();
        AstContext astContext = new AstContext(getSqlClient());
        SqlBuilder builder = new SqlBuilder(astContext);
        astContext.pushStatement(this);
        try {
            MutationQueryAnalyzer.apply(builder, analysisAst, sourceAst);
        } finally {
            astContext.popStatement();
        }
        freeze(astContext);
        return builder;
    }

    abstract void validateSemantics();

    abstract boolean isUpsert();

    abstract boolean isConflictIgnored();

    abstract List<Target> conflictTargets();

    private void validateCommand(@Nullable List<Selection<?>> returningSelections) {
        addImplicitAssignments();
        if (assignments.isEmpty()) {
            throw new IllegalStateException("At least one insert assignment is required");
        }
        validateSemantics();
        if (returningSelections != null) {
            if (returningSelections.isEmpty()) {
                throw new IllegalArgumentException("The returning selection list cannot be empty");
            }
            for (Selection<?> selection : returningSelections) {
                validateReturningSelection(selection);
            }
        }
    }

    private void addImplicitAssignments() {
        if (implicitAssignmentsAdded) {
            return;
        }
        implicitAssignmentsAdded = true;
        ImmutableType type = getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        if (inheritanceInfo != null && type.getDiscriminatorValue() != null) {
            ImmutableProp prop = inheritanceInfo.getDiscriminatorProp(type);
            Object value = inheritanceInfo.getDiscriminatorValue(type);
            addImplicit(prop, value);
            if (isUpsert()) {
                PropExpression<Object> expression = ((Table<?>) getTable()).get(prop);
                discriminatorUpdatePredicate = expression.eq(value);
            }
        }
        LogicalDeletedInfo logicalDeletedInfo = type.getLogicalDeletedInfo();
        if (logicalDeletedInfo != null) {
            addImplicit(logicalDeletedInfo.getProp(), logicalDeletedInfo.allocateInitializedValue());
        }
        ImmutableProp versionProp = type.getVersionProp();
        if (versionProp != null && assignmentByProp(versionProp) == null) {
            addImplicit(versionProp, 0);
        }
        ImmutableProp idProp = type instanceof AssociationType ? null : type.getIdProp();
        if (idProp != null && assignmentByProp(idProp) == null) {
            IdGenerator generator = getSqlClient().getGeneratorContext().getIdGenerator(type);
            if (generator instanceof SequenceIdGenerator) {
                String sql = getSqlClient().getDialect().getSelectIdFromSequenceSql(
                        ((SequenceIdGenerator) generator).getSequenceName()
                );
                if (sql.regionMatches(true, 0, "select ", 0, 7)) {
                    sql = sql.substring(7);
                }
                int fromIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf(" from ");
                if (fromIndex != -1) {
                    sql = sql.substring(0, fromIndex);
                }
                addImplicit(idProp, new NativeSqlExpression<>(idProp.getElementClass(), sql));
                Assignment assignment = assignmentByProp(idProp);
                if (assignment != null) {
                    implicitSequenceIdTarget = assignment.target;
                }
            } else if (generator != null && !(generator instanceof IdentityIdGenerator)) {
                materializedIdRequired = true;
            }
        }
    }

    private boolean isMaterializedExecutionRequired(@Nullable List<Selection<?>> returningSelections) {
        Dialect dialect = getSqlClient().getDialect();
        if (materializedIdRequired) {
            return true;
        }
        if (getType() instanceof AssociationType) {
            AssociationType associationType = (AssociationType) getType();
            if (associationType.getJoinTableDeletedInfo() != null ||
                    associationType.getJoinTableFilterInfo() != null) {
                return true;
            }
        }
        InsertFromSelectContext ctx = new NativeInsertFromSelectContext(null, returningSelections);
        InsertFromSelectRenderer renderer = dialect.getInsertFromSelectRenderer();
        if (getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY &&
                !renderer.isTransactionTriggerSupported(ctx)) {
            return true;
        }
        return !renderer.isSupported(ctx);
    }

    private boolean isUpdateExpressionAliasingSupported() {
        Set<Object> availableSources = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Assignment assignment : assignments.values()) {
            if (assignment.target.definition.size() == 1) {
                availableSources.add(assignment.insertSource);
            }
        }
        for (Expression<?> expression : collectMaterializedSourceExpressions()) {
            if (!availableSources.contains(expression)) {
                return false;
            }
        }
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.MERGE && assignment.target.definition.size() != 1) {
                return false;
            }
        }
        return true;
    }

    private boolean isSimpleInsertedValueUpdate() {
        if (hasNativeUpdatePredicates()) {
            return false;
        }
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.MERGE &&
                    (assignment.target.definition.size() != 1 ||
                            assignment.updateExpression != assignment.insertSource)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNativeUpdatePredicates() {
        return discriminatorUpdatePredicate != null || !updatePredicates.isEmpty();
    }

    private List<Target> nativeConflictTargets() {
        List<Target> targets = conflictTargets();
        if (targets.isEmpty() || isIdConflict(targets)) {
            return targets;
        }
        List<ImmutableProp> keyProps = new ArrayList<>(targets.size());
        for (Target target : targets) {
            keyProps.add(target.prop);
        }
        List<ImmutableProp> conflictProps = MutationKeys.keyAndLogicalDeletedProps(getType(), keyProps);
        if (conflictProps.size() == keyProps.size() || nativeConflictPredicate() != null) {
            return targets;
        }
        List<Target> nativeTargets = new ArrayList<>(targets);
        for (ImmutableProp conflictProp : conflictProps) {
            Assignment assignment = assignmentByProp(conflictProp);
            if (assignment != null && !nativeTargets.contains(assignment.target)) {
                nativeTargets.add(assignment.target);
            }
        }
        return nativeTargets;
    }

    @Nullable
    private LogicalDeletedInfo nativeConflictPredicate() {
        List<Target> targets = conflictTargets();
        if (targets.isEmpty() || isIdConflict(targets)) {
            return null;
        }
        return MutationKeys.logicalDeletedConflictPredicate(getType());
    }

    private boolean isIdConflict(Collection<Target> targets) {
        ImmutableProp idProp = getType() instanceof AssociationType ? null : getType().getIdProp();
        if (idProp == null) {
            return false;
        }
        Set<String> columns = new LinkedHashSet<>();
        for (Target target : targets) {
            columns.addAll(target.columnNames(getSqlClient().getMetadataStrategy()));
        }
        return columns.equals(columnNames(Collections.singleton(idProp)));
    }

    private boolean isConflictTargetUnambiguous() {
        Set<String> selectedColumns = new LinkedHashSet<>();
        for (Target target : conflictTargets()) {
            selectedColumns.addAll(target.columnNames(getSqlClient().getMetadataStrategy()));
        }
        ImmutableProp idProp = getType() instanceof AssociationType ? null : getType().getIdProp();
        if (idProp != null && selectedColumns.equals(columnNames(Collections.singleton(idProp)))) {
            return getType().getKeyMatcher().toMap().isEmpty();
        }
        if (idProp != null && assignmentByProp(idProp) != null) {
            return false;
        }
        ImmutableType keyConstraintType = keyConstraintType();
        KeyUniqueConstraint constraint = keyUniqueConstraint(keyConstraintType);
        if (constraint == null || !constraint.noMoreUniqueConstraints()) {
            return false;
        }
        Map<String, Set<ImmutableProp>> keyGroups = keyConstraintType.getKeyMatcher().toMap();
        return keyGroups.size() == 1 && selectedColumns.equals(columnNames(keyGroups.values().iterator().next()));
    }

    private boolean isNullableConflictTargetSupported() {
        boolean nullable = false;
        for (Target target : nativeConflictTargets()) {
            if (target.prop.isNullable()) {
                nullable = true;
                break;
            }
        }
        if (!nullable) {
            return true;
        }
        KeyUniqueConstraint constraint = keyUniqueConstraint(keyConstraintType());
        return constraint != null &&
                constraint.isNullNotDistinct() &&
                getSqlClient().getDialect().isUpsertWithNullableKeySupported();
    }

    private ImmutableType keyConstraintType() {
        ImmutableType type = getType();
        InheritanceInfo inheritanceInfo = type.getInheritanceInfo();
        return inheritanceInfo != null ? inheritanceInfo.getRootType() : type;
    }

    @Nullable
    private KeyUniqueConstraint keyUniqueConstraint(ImmutableType keyConstraintType) {
        KeyUniqueConstraint constraint = getType().getJavaClass().getAnnotation(KeyUniqueConstraint.class);
        if (constraint == null && keyConstraintType != getType()) {
            constraint = keyConstraintType.getJavaClass().getAnnotation(KeyUniqueConstraint.class);
        }
        return constraint;
    }

    private List<Expression<?>> collectMaterializedSourceExpressions() {
        AstContext ctx = new AstContext(getSqlClient());
        List<Expression<?>> expressions = new ArrayList<>();
        Set<Expression<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Expression<?>> loadedSources = new HashSet<>();
        for (Assignment assignment : assignments.values()) {
            loadedSources.add(assignment.insertSource);
        }
        AstVisitor visitor = new AstVisitor(ctx) {
            @Override
            public void visitBaseTableExpression(
                    BaseTableOwner baseTableOwner,
                    Expression<?> expression
            ) {
                if (baseTableOwner.getBaseTable() == sourceSymbol &&
                        !loadedSources.contains(expression) &&
                        visited.add(expression)) {
                    expressions.add(expression);
                }
            }
        };
        ctx.pushStatement(this);
        ctx.pushStatement(sourceQuery);
        try {
            for (Assignment assignment : assignments.values()) {
                if (assignment.updateExpression != null &&
                        assignment.updateExpression != assignment.insertSource) {
                    ((Ast) assignment.updateExpression).accept(visitor);
                }
            }
            for (Predicate predicate : updatePredicates) {
                ((Ast) predicate).accept(visitor);
            }
        } finally {
            ctx.popStatement();
            ctx.popStatement();
        }
        return expressions;
    }

    private InsertFromSelectMode mutationMode() {
        return isUpsert() ?
                InsertFromSelectMode.UPSERT :
                isConflictIgnored() ?
                        InsertFromSelectMode.INSERT_IF_ABSENT :
                        InsertFromSelectMode.INSERT;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <R> MaterializedResult<R> executeMaterialized(
            Connection con,
            SqlBuilder builder,
            @Nullable List<Selection<?>> returningSelections,
            @Nullable TupleCreator<R> tupleCreator
    ) {
        List<Selection<?>> sourceSelections = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments.values()) {
            sourceSelections.add(assignment.insertSource);
        }
        for (Expression<?> expression : materializedSourceExpressions) {
            sourceSelections.add(expression);
        }
        renderSourceSelect(builder, sourceSelections);
        Tuple3<String, List<Object>, List<Integer>> sql = builder.build();
        List<Object[]> sourceRows = Selectors.select(
                getSqlClient(),
                con,
                sql.get_1(),
                sql.get_2(),
                sql.get_3(),
                sourceSelections,
                args -> args,
                ExecutionPurpose.command(QueryReason.NONE)
        );
        if (isUpsert()) {
            validateDistinctMaterializedKeys(sourceRows);
        }
        if (getType() instanceof AssociationType) {
            return executeMaterializedAssociation(
                    con,
                    sourceRows,
                    returningSelections,
                    tupleCreator
            );
        }
        int affectedRowCount = 0;
        List<Object> acceptedEntities = returningSelections != null ? new ArrayList<>() : null;
        for (Object[] row : sourceRows) {
            Object entity = createMaterializedEntity(row);
            SimpleEntitySaveCommand<Object> command = getSqlClient()
                    .saveCommand(entity)
                    .setMode(
                            isUpsert() ?
                                    SaveMode.UPSERT :
                                    isConflictIgnored() ?
                                            SaveMode.INSERT_IF_ABSENT :
                                            SaveMode.INSERT_ONLY
                    )
                    .setVersionMode(VersionMode.ASSIGNMENT);
            SaveCommandImplementor implementor = (SaveCommandImplementor) command;
            if (isUpsert() || isConflictIgnored()) {
                implementor = (SaveCommandImplementor) implementor.setExactConflictTargetRequired();
            }
            if (isUpsert()) {
                implementor = (SaveCommandImplementor) implementor.setForceMatchedUpdate();
            }
            command = (SimpleEntitySaveCommand<Object>) implementor;
            if (isUpsert() || isConflictIgnored()) {
                List<Target> conflictTargets = conflictTargets();
                ImmutableProp idProp = getType().getIdProp();
                Set<String> conflictColumns = new LinkedHashSet<>();
                LinkedHashSet<ImmutableProp> keyPropSet = new LinkedHashSet<>();
                for (Target conflictTarget : conflictTargets) {
                    conflictColumns.addAll(
                            conflictTarget.columnNames(getSqlClient().getMetadataStrategy())
                    );
                    keyPropSet.add(materializedProp(conflictTarget.prop));
                }
                if (idProp == null ||
                        !conflictColumns.equals(columnNames(Collections.singleton(idProp)))) {
                    ImmutableProp[] keyProps = keyPropSet.toArray(new ImmutableProp[0]);
                    if (keyProps.length == 0) {
                        throw new AssertionError("Internal bug: No materialized conflict key property");
                    }
                    command = command.setKeyProps(keyProps);
                }
            }
            if (isUpsert()) {
                UpsertMask mask = UpsertMask
                        .of((Class) getType().getJavaClass())
                        .forbidInsert()
                        .forbidUpdate();
                for (Assignment assignment : assignments.values()) {
                    ImmutableProp maskProp = materializedProp(assignment.target.prop);
                    mask = mask.addInsertableProp(maskProp);
                    if (assignment.role == Role.MERGE) {
                        mask = mask.addUpdatableProp(maskProp);
                    }
                }
                command = command.setUpsertMask(mask);
                command = configureMaterializedSaveCommand(command, row);
            }
            SimpleSaveResult<Object> result = command.execute(con);
            affectedRowCount += result.getTotalAffectedRowCount();
            if (acceptedEntities != null && result.isAccepted()) {
                acceptedEntities.add(result.getModifiedEntity());
            }
        }
        List<R> rows = returningSelections != null ?
                mapEntityReturning(con, acceptedEntities, returningSelections, tupleCreator) :
                Collections.emptyList();
        return new MaterializedResult<>(affectedRowCount, rows);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SimpleEntitySaveCommand<Object> configureMaterializedSaveCommand(
            SimpleEntitySaveCommand<Object> command,
            Object[] row
    ) {
        SaveCommandImplementor implementor = (SaveCommandImplementor) command;
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.MERGE &&
                    assignment.updateExpression != null &&
                    assignment.updateExpression != assignment.insertSource) {
                ExpressionImplementor<?> raw = (ExpressionImplementor<?>) assignment.updateExpression;
                implementor = (SaveCommandImplementor) implementor.setEntityAssignment(
                        materializedProp(assignment.target.prop),
                        (SaveAssignmentExpression) (target, values) ->
                                new MaterializedExpression(
                                        raw,
                                        materializedSaveReplacements(row, target, values)
                                )
                );
            }
        }
        if (!updatePredicates.isEmpty()) {
            PredicateImplementor raw = (PredicateImplementor) Predicate.and(
                    updatePredicates.toArray(new Predicate[0])
            );
            implementor = (SaveCommandImplementor) implementor.setEntityUpdateWhere(
                    getType(),
                    (UpdateCondition) (target, values) ->
                            new MaterializedPredicate(
                                    raw,
                                    materializedSaveReplacements(row, target, values)
                            )
            );
        }
        return (SimpleEntitySaveCommand<Object>) implementor;
    }

    private Map<Object, Object> materializedSaveReplacements(
            Object[] row,
            Table<?> target,
            ValueExpressionFactory<?> values
    ) {
        Map<Object, Object> replacements = materializedSourceReplacements(
                row,
                materializedSourceExpressions
        );
        replacements.put(getTable(), target);
        for (Assignment assignment : assignments.values()) {
            Expression<?> replacement;
            ImmutableProp prop = materializedProp(assignment.target.prop);
            if (assignment.target.expr.getPath() == null &&
                    !prop.isAssociation(TargetLevel.PERSISTENT) &&
                    !prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                replacement = values.newValue(prop);
            } else {
                replacement = materializedLiteral(
                        assignment.insertSource,
                        row[assignmentIndex(assignment)]
                );
            }
            replacements.put(assignment.insertSource, replacement);
        }
        return replacements;
    }

    private Object createMaterializedEntity(Object[] row) {
        return Internal.produce(getType(), null, draft -> {
            DraftSpi spi = (DraftSpi) draft;
            int index = 0;
            Map<ImmutableProp, Map<String, Object>> embeddedValues = new LinkedHashMap<>();
            for (Assignment assignment : assignments.values()) {
                Object value = row[index++];
                ImmutableProp prop = materializedProp(assignment.target.prop);
                String path = assignment.target.expr.getPath();
                if (path != null) {
                    embeddedValues
                            .computeIfAbsent(prop, it -> new LinkedHashMap<>())
                            .put(path, value);
                    continue;
                }
                if (prop.isAssociation(TargetLevel.ENTITY) && value != null) {
                    value = ImmutableObjects.makeIdOnly(prop.getTargetType(), value);
                }
                spi.__set(prop.getId(), value);
            }
            for (Map.Entry<ImmutableProp, Map<String, Object>> e : embeddedValues.entrySet()) {
                spi.__set(
                        e.getKey().getId(),
                        createMaterializedEmbedded(e.getKey().getTargetType(), e.getValue())
                );
            }
        });
    }

    private ImmutableProp materializedProp(ImmutableProp prop) {
        ImmutableProp actualProp = getType().getProps().get(prop.getName());
        return actualProp != null ? actualProp : prop;
    }

    private Object createMaterializedEmbedded(
            ImmutableType type,
            Map<String, Object> pathValues
    ) {
        return Internal.produce(type, null, draft -> {
            DraftSpi spi = (DraftSpi) draft;
            Map<String, Map<String, Object>> nestedValues = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : pathValues.entrySet()) {
                String path = e.getKey();
                int dotIndex = path.indexOf('.');
                if (dotIndex == -1) {
                    ImmutableProp prop = type.getProp(path);
                    spi.__set(prop.getId(), e.getValue());
                } else {
                    nestedValues
                            .computeIfAbsent(path.substring(0, dotIndex), it -> new LinkedHashMap<>())
                            .put(path.substring(dotIndex + 1), e.getValue());
                }
            }
            for (Map.Entry<String, Map<String, Object>> e : nestedValues.entrySet()) {
                ImmutableProp prop = type.getProp(e.getKey());
                spi.__set(
                        prop.getId(),
                        createMaterializedEmbedded(prop.getTargetType(), e.getValue())
                );
            }
        });
    }

    private Map<Object, Object> materializedSourceReplacements(
            Object[] row,
            List<Expression<?>> sourceExpressions
    ) {
        Map<Object, Object> replacements = new HashMap<>();
        int offset = assignments.size();
        for (int i = 0; i < sourceExpressions.size(); i++) {
            Expression<?> sourceExpression = sourceExpressions.get(i);
            Object value = row[offset + i];
            Expression<?> replacement = materializedLiteral(sourceExpression, value);
            replacements.put(sourceExpression, replacement);
        }
        return replacements;
    }

    private static Expression<?> materializedLiteral(Expression<?> expression, Object value) {
        return value != null ?
                Expression.value(value) :
                Expression.nullValue(((ExpressionImplementor<?>) expression).getType());
    }

    private Object findMaterializedEntityById(Connection con, Object id) {
        MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(
                getSqlClient(),
                (TableProxy<?>) getTable(),
                ExecutionPurpose.MUTATE,
                FilterLevel.IGNORE_ALL
        );
        Table<?> table = query.getTable();
        query.where(table.get(getType().getIdProp()).eq(id));
        List<?> entities = query.select(table).execute(con);
        if (entities.isEmpty()) {
            throw new ExecutionException(
                    "Cannot reload the row mutated by insert/upsert-from-select, id: " + id
            );
        }
        return entities.get(0);
    }


    private void validateDistinctMaterializedKeys(List<Object[]> rows) {
        List<Integer> keyIndexes = new ArrayList<>();
        int index = 0;
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.KEY) {
                keyIndexes.add(index);
            }
            index++;
        }
        Set<List<Object>> keys = new HashSet<>();
        for (Object[] row : rows) {
            List<Object> key = new ArrayList<>(keyIndexes.size());
            for (Integer keyIndex : keyIndexes) {
                key.add(row[keyIndex]);
            }
            if (!keys.add(key)) {
                throw new IllegalArgumentException(
                        "The materialized upsert source contains duplicate key " + key
                );
            }
        }
    }

    private <R> MaterializedResult<R> executeMaterializedAssociation(
            Connection con,
            List<Object[]> sourceRows,
            @Nullable List<Selection<?>> returningSelections,
            @Nullable TupleCreator<R> tupleCreator
    ) {
        AssociationType associationType = (AssociationType) getType();
        Assignment sourceAssignment = assignmentByProp(associationType.getSourceProp());
        Assignment targetAssignment = assignmentByProp(associationType.getTargetProp());
        int sourceIndex = assignmentIndex(sourceAssignment);
        int targetIndex = assignmentIndex(targetAssignment);
        Associations associations = getSqlClient()
                .getAssociations(associationType)
                .forConnection(con);
        int affectedRowCount = 0;
        List<Object[]> acceptedRows = returningSelections != null ? new ArrayList<>() : null;
        for (Object[] row : sourceRows) {
            AssociationSaveCommandImpl command = (AssociationSaveCommandImpl) associations
                    .saveCommand(row[sourceIndex], row[targetIndex])
                    .ignoreConflict(isConflictIgnored())
                    .deleteUnnecessary(false);
            int count = isConflictIgnored() ?
                    command.executeWithCheckingExistence(con) :
                    command.execute(con);
            affectedRowCount += count;
            if (acceptedRows != null && (!isConflictIgnored() || count != 0)) {
                acceptedRows.add(row);
            }
        }
        if (returningSelections == null) {
            return new MaterializedResult<>(affectedRowCount, Collections.emptyList());
        }
        List<R> rows = new ArrayList<>(acceptedRows.size());
        for (Object[] acceptedRow : acceptedRows) {
            Object[] values = new Object[returningSelections.size()];
            for (int i = 0; i < values.length; i++) {
                Target target = Target.of(
                        (PropExpression<?>) returningSelections.get(i),
                        getSqlClient().getMetadataStrategy()
                );
                values[i] = target.prop.toOriginal() == associationType.getSourceProp().toOriginal() ?
                        acceptedRow[sourceIndex] :
                        acceptedRow[targetIndex];
            }
            rows.add(toReturningRow(values, tupleCreator));
        }
        return new MaterializedResult<>(affectedRowCount, rows);
    }

    private int assignmentIndex(Assignment expected) {
        int index = 0;
        for (Assignment assignment : assignments.values()) {
            if (assignment == expected) {
                return index;
            }
            index++;
        }
        throw new AssertionError("Internal bug: assignment is not part of the command");
    }

    private <R> List<R> mapEntityReturning(
            Connection con,
            List<Object> entities,
            List<Selection<?>> returningSelections,
            @Nullable TupleCreator<R> tupleCreator
    ) {
        List<R> rows = new ArrayList<>(entities.size());
        for (Object entity : entities) {
            ImmutableSpi spi = (ImmutableSpi) entity;
            for (Selection<?> selection : returningSelections) {
                Target target = Target.of(
                        (PropExpression<?>) selection,
                        getSqlClient().getMetadataStrategy()
                );
                if (!isMaterializedTargetLoaded(spi, target)) {
                    Object id = spi.__get(getType().getIdProp().getId());
                    spi = (ImmutableSpi) findMaterializedEntityById(con, id);
                    break;
                }
            }
            Object[] values = new Object[returningSelections.size()];
            for (int i = 0; i < values.length; i++) {
                Target target = Target.of(
                        (PropExpression<?>) returningSelections.get(i),
                        getSqlClient().getMetadataStrategy()
                );
                Object value = materializedTargetValue(spi, target);
                if (target.prop.isAssociation(TargetLevel.ENTITY) && value != null) {
                    ImmutableSpi targetSpi = (ImmutableSpi) value;
                    value = targetSpi.__get(target.prop.getTargetType().getIdProp().getId());
                }
                values[i] = value;
            }
            rows.add(toReturningRow(values, tupleCreator));
        }
        return rows;
    }

    private static boolean isMaterializedTargetLoaded(ImmutableSpi spi, Target target) {
        if (!spi.__isLoaded(target.prop.getId())) {
            return false;
        }
        Object value = spi.__get(target.prop.getId());
        String path = target.expr.getPath();
        if (path == null || value == null) {
            return true;
        }
        ImmutableType type = target.prop.getTargetType();
        ImmutableSpi nested = (ImmutableSpi) value;
        for (String part : path.split("\\.")) {
            ImmutableProp prop = type.getProp(part);
            if (!nested.__isLoaded(prop.getId())) {
                return false;
            }
            value = nested.__get(prop.getId());
            if (value == null) {
                return true;
            }
            type = prop.getTargetType();
            if (type != null) {
                nested = (ImmutableSpi) value;
            }
        }
        return true;
    }

    private static Object materializedTargetValue(ImmutableSpi spi, Target target) {
        Object value = spi.__get(target.prop.getId());
        String path = target.expr.getPath();
        if (path == null || value == null) {
            return value;
        }
        ImmutableType type = target.prop.getTargetType();
        ImmutableSpi nested = (ImmutableSpi) value;
        for (String part : path.split("\\.")) {
            ImmutableProp prop = type.getProp(part);
            value = nested.__get(prop.getId());
            if (value == null) {
                return null;
            }
            type = prop.getTargetType();
            if (type != null) {
                nested = (ImmutableSpi) value;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static <R> R toReturningRow(Object[] values, @Nullable TupleCreator<R> tupleCreator) {
        if (tupleCreator != null) {
            return tupleCreator.createTuple(values);
        }
        switch (values.length) {
            case 1:
                return (R) values[0];
            case 2:
                return (R) new Tuple2<>(values[0], values[1]);
            case 3:
                return (R) new Tuple3<>(values[0], values[1], values[2]);
            case 4:
                return (R) new Tuple4<>(values[0], values[1], values[2], values[3]);
            case 5:
                return (R) new Tuple5<>(values[0], values[1], values[2], values[3], values[4]);
            case 6:
                return (R) new Tuple6<>(values[0], values[1], values[2], values[3], values[4], values[5]);
            case 7:
                return (R) new Tuple7<>(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
            case 8:
                return (R) new Tuple8<>(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7]);
            case 9:
                return (R) new Tuple9<>(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8]);
            default:
                throw new AssertionError("Internal bug: Illegal returning value count " + values.length);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addImplicit(ImmutableProp prop, Object value) {
        if (assignmentByProp(prop) != null) {
            return;
        }
        PropExpression expression = ((Table<?>) getTable()).get(prop);
        Expression sourceExpression = value instanceof Expression<?> ?
                (Expression<?>) value :
                Expression.any().value(value);
        addAssignment(expression, sourceExpression, null, Role.INSERT);
    }

    final Assignment assignmentByProp(ImmutableProp prop) {
        ImmutableProp original = prop.toOriginal();
        for (Assignment assignment : assignments.values()) {
            if (assignment.target.prop.toOriginal() == original) {
                return assignment;
            }
        }
        return null;
    }

    final List<Target> validateUniqueTargets(Collection<Target> targets, String label) {
        if (targets.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty");
        }
        LinkedHashSet<Target> distinctTargets = new LinkedHashSet<>();
        for (Target target : targets) {
            if (!distinctTargets.add(target)) {
                throw new IllegalArgumentException(
                        "Duplicate " + label + " property \"" + target.expr + "\""
                );
            }
            if (!assignments.containsKey(target)) {
                throw new IllegalArgumentException(
                        "The " + label + " property \"" + target.expr + "\" has no insert assignment"
                );
            }
        }
        if (!isUnique(targets)) {
            throw new IllegalArgumentException(
                    "The " + label + " properties " + targets + " do not uniquely identify a target row"
            );
        }
        return new ArrayList<>(targets);
    }

    private boolean isUnique(Collection<Target> targets) {
        ImmutableType type = getType();
        Set<String> columns = new LinkedHashSet<>();
        for (Target target : targets) {
            columns.addAll(target.columnNames(getSqlClient().getMetadataStrategy()));
        }
        ImmutableProp idProp = type instanceof AssociationType ? null : type.getIdProp();
        if (idProp != null && columns.equals(columnNames(Collections.singleton(idProp)))) {
            return true;
        }
        if (type instanceof AssociationType) {
            return columns.equals(columnNames(Arrays.asList(
                    type.getProp("source"),
                    type.getProp("target")
            )));
        }
        for (Set<ImmutableProp> group : type.getKeyMatcher().toMap().values()) {
            if (columns.equals(columnNames(group))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> columnNames(Collection<ImmutableProp> props) {
        Set<String> names = new LinkedHashSet<>();
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        for (ImmutableProp prop : props) {
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof ColumnDefinition) {
                for (String name : (ColumnDefinition) storage) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    final List<Target> inferConflictTargets() {
        ImmutableType type = getType();
        ImmutableProp idProp = type instanceof AssociationType ? null : type.getIdProp();
        if (idProp != null) {
            List<Target> idTargets = targetsForUniqueProps(Collections.singleton(idProp));
            if (!idTargets.isEmpty()) {
                return idTargets;
            }
        }
        if (type instanceof AssociationType) {
            List<Target> associationTargets = targetsForUniqueProps(Arrays.asList(
                    type.getProp("source"),
                    type.getProp("target")
            ));
            if (!associationTargets.isEmpty()) {
                return associationTargets;
            }
        }
        for (Set<ImmutableProp> group : type.getKeyMatcher().toMap().values()) {
            List<Target> targets = targetsForUniqueProps(group);
            if (!targets.isEmpty()) {
                return targets;
            }
        }
        throw new IllegalStateException(
                "Cannot infer an eligible conflict key for target type \"" + type + "\""
        );
    }

    private List<Target> targetsForUniqueProps(Collection<ImmutableProp> props) {
        Set<ImmutableProp> originals = new LinkedHashSet<>();
        for (ImmutableProp prop : props) {
            originals.add(prop.toOriginal());
        }
        List<Target> targets = new ArrayList<>();
        Set<String> assignedColumns = new LinkedHashSet<>();
        for (Assignment assignment : assignments.values()) {
            if (originals.contains(assignment.target.prop.toOriginal())) {
                targets.add(assignment.target);
                assignedColumns.addAll(
                        assignment.target.columnNames(getSqlClient().getMetadataStrategy())
                );
            }
        }
        return assignedColumns.equals(columnNames(props)) ? targets : Collections.emptyList();
    }

    private void validateReturningSelection(Selection<?> selection) {
        if (!(selection instanceof PropExpression<?>)) {
            throw new IllegalArgumentException(
                    "Insert/upsert returning supports only physical target properties"
            );
        }
        Target target = Target.of(
                (PropExpression<?>) selection,
                getSqlClient().getMetadataStrategy()
        );
        validateTarget(target);
    }

    private void renderMutation(SqlBuilder builder, @Nullable List<Selection<?>> returningSelections) {
        Dialect dialect = getSqlClient().getDialect();
        InsertFromSelectContext ctx = new NativeInsertFromSelectContext(builder, returningSelections);
        InsertFromSelectRenderer renderer = dialect.getInsertFromSelectRenderer();
        if (!renderer.isSupported(ctx)) {
            throw unsupported("native " + mutationMode().name().toLowerCase(Locale.ROOT));
        }
        renderer.render(ctx);
    }

    private void renderSourceSelect(SqlBuilder builder) {
        List<Selection<?>> selections = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments.values()) {
            selections.add(assignment.insertSource);
        }
        renderSourceSelect(builder, selections);
    }

    private void renderSourceSelect(SqlBuilder builder, List<Selection<?>> selections) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(this);
        astContext.pushStatement(sourceQuery);
        try {
            builder.enter(SqlBuilder.ScopeType.SELECT);
            for (Selection<?> selection : selections) {
                builder.separator();
                renderExpression(builder, (Expression<?>) selection, true);
            }
            builder.leave();
            builder.from();
            sourceTable.realTable(builder.getQueryRenderContext()).renderTo(builder, false);
        } finally {
            astContext.popStatement();
            astContext.popStatement();
        }
    }

    private List<Target> assignmentTargets() {
        List<Target> targets = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments.values()) {
            targets.add(assignment.target);
        }
        return targets;
    }

    private List<Assignment> mergeAssignments() {
        List<Assignment> list = new ArrayList<>();
        for (Assignment assignment : assignments.values()) {
            if (assignment.role == Role.MERGE) {
                list.add(assignment);
            }
        }
        return list;
    }

    private Target fakeUpdateTarget() {
        ImmutableProp prop = ((ImmutableTypeImplementor) getType()).getFakeUpdateProp();
        if (prop == null) {
            prop = getType().getIdProp();
        }
        if (prop == null) {
            throw unsupported("fake update column selection");
        }
        return Target.of(((Table<?>) getTable()).get(prop), getSqlClient().getMetadataStrategy());
    }

    private void renderTargets(SqlBuilder builder, Collection<Target> targets, @Nullable String prefix) {
        for (Target target : targets) {
            builder.separator();
            renderTarget(builder, target, prefix);
        }
    }

    private void renderTarget(SqlBuilder builder, Target target, @Nullable String prefix) {
        builder.definition(prefix, target.definition);
    }

    private void renderExpression(SqlBuilder builder, Expression<?> expression, boolean ignoreBrackets) {
        if (expression instanceof PropExpressionImplementor<?>) {
            ((PropExpressionImplementor<?>) expression).renderTo(builder, ignoreBrackets);
        } else {
            ((Ast) expression).renderTo(builder);
        }
    }

    private void renderReturningSelections(SqlBuilder builder, List<Selection<?>> selections) {
        builder.enter(SqlBuilder.ScopeType.COMMA);
        for (Selection<?> selection : selections) {
            Target target = Target.of(
                    (PropExpression<?>) selection,
                    getSqlClient().getMetadataStrategy()
            );
            builder.separator();
            renderTarget(builder, target, null);
        }
        builder.leave();
    }

    private String targetAlias(SqlBuilder builder) {
        return builder.alias(getTableLikeImplementor().realTable(builder.getQueryRenderContext()));
    }

    private final class NativeInsertFromSelectContext implements InsertFromSelectContext {

        @Nullable
        private final SqlBuilder builder;

        @Nullable
        private final List<Selection<?>> returningSelections;

        private NativeInsertFromSelectContext(
                @Nullable SqlBuilder builder,
                @Nullable List<Selection<?>> returningSelections
        ) {
            this.builder = builder;
            this.returningSelections = returningSelections;
        }

        @Override
        public InsertFromSelectMode getMode() {
            return mutationMode();
        }

        @Override
        public boolean hasReturning() {
            return returningSelections != null;
        }

        @Override
        public boolean hasUpdateAssignments() {
            return !mergeAssignments().isEmpty();
        }

        @Override
        public boolean hasUpdatePredicates() {
            return hasNativeUpdatePredicates();
        }

        @Override
        public boolean hasConflictPredicate() {
            return nativeConflictPredicate() != null;
        }

        @Override
        public boolean isUpdateExpressionAliasingSupported() {
            return AbstractInsertFromSelectImpl.this.isUpdateExpressionAliasingSupported();
        }

        @Override
        public boolean isSimpleInsertedValueUpdate() {
            return AbstractInsertFromSelectImpl.this.isSimpleInsertedValueUpdate();
        }

        @Override
        public boolean isConflictTargetUnambiguous() {
            return AbstractInsertFromSelectImpl.this.isConflictTargetUnambiguous();
        }

        @Override
        public boolean isNullableConflictTargetSupported() {
            return AbstractInsertFromSelectImpl.this.isNullableConflictTargetSupported();
        }

        @Override
        public InsertFromSelectContext sql(String sql) {
            builder().sql(sql);
            return this;
        }

        @Override
        public InsertFromSelectContext enter(AbstractSqlBuilder.ScopeType type) {
            builder().enter(type);
            return this;
        }

        @Override
        public InsertFromSelectContext separator() {
            builder().separator();
            return this;
        }

        @Override
        public InsertFromSelectContext leave() {
            builder().leave();
            return this;
        }

        @Override
        public InsertFromSelectContext appendTableName() {
            builder().sql(getType().getTableName(getSqlClient().getMetadataStrategy()));
            return this;
        }

        @Override
        public InsertFromSelectContext appendTargetAlias() {
            SqlBuilder builder = builder();
            builder.sql(targetAlias(builder));
            return this;
        }

        @Override
        public InsertFromSelectContext appendInsertColumns() {
            renderTargets(builder(), assignmentTargets(), null);
            return this;
        }

        @Override
        public InsertFromSelectContext appendConflictColumns() {
            renderTargets(builder(), nativeConflictTargets(), null);
            return this;
        }

        @Override
        public InsertFromSelectContext appendConflictPredicate(boolean targetAlias) {
            LogicalDeletedInfo logicalDeletedInfo = nativeConflictPredicate();
            if (logicalDeletedInfo == null) {
                throw new IllegalStateException("No conflict predicate is available");
            }
            SqlBuilder builder = builder();
            builder.logicalDeleteConflictPredicate(
                    logicalDeletedInfo,
                    targetAlias ? targetAlias(builder) : null
            );
            return this;
        }

        @Override
        public InsertFromSelectContext appendSourceSelect() {
            renderSourceSelect(builder());
            return this;
        }

        @Override
        public InsertFromSelectContext appendSourceTable() {
            SqlBuilder builder = builder();
            withSourceScope(() ->
                    sourceTable.realTable(builder.getQueryRenderContext()).renderTo(builder, false)
            );
            return this;
        }

        @Override
        public InsertFromSelectContext appendConflictCondition() {
            SqlBuilder builder = builder();
            withSourceScope(() -> {
                String targetAlias = targetAlias(builder);
                for (Target conflictTarget : nativeConflictTargets()) {
                    Assignment assignment = assignments.get(conflictTarget);
                    builder.separator();
                    renderTarget(builder, conflictTarget, targetAlias);
                    builder.sql(" = ");
                    renderExpression(builder, assignment.insertSource, false);
                }
            });
            return this;
        }

        @Override
        public InsertFromSelectContext appendInsertValues() {
            SqlBuilder builder = builder();
            withSourceScope(() -> {
                for (Assignment assignment : assignments.values()) {
                    builder.separator();
                    renderExpression(builder, assignment.insertSource, true);
                }
            });
            return this;
        }

        @Override
        public InsertFromSelectContext appendUpdateAssignments(
                boolean targetAlias,
                @Nullable String insertedValuePrefix,
                @Nullable String insertedValueSuffix
        ) {
            SqlBuilder builder = builder();
            withSourceScope(insertedValueReplacements(insertedValuePrefix, insertedValueSuffix), () -> {
                String targetPrefix = targetAlias ? targetAlias(builder) : null;
                for (Assignment assignment : mergeAssignments()) {
                    builder.separator();
                    renderTarget(builder, assignment.target, targetPrefix);
                    builder.sql(" = ");
                    renderExpression(builder, assignment.updateExpression, false);
                }
            });
            return this;
        }

        @Override
        public InsertFromSelectContext appendFakeUpdateAssignment(
                boolean leftTargetAlias,
                boolean rightTargetAlias
        ) {
            SqlBuilder builder = builder();
            Target fake = fakeUpdateTarget();
            renderTarget(builder, fake, leftTargetAlias ? targetAlias(builder) : null);
            builder.sql(" = ");
            renderTarget(builder, fake, rightTargetAlias ? targetAlias(builder) : null);
            return this;
        }

        @Override
        public InsertFromSelectContext appendUpdatePredicates(
                @Nullable String insertedValuePrefix,
                @Nullable String insertedValueSuffix
        ) {
            SqlBuilder builder = builder();
            withSourceScope(insertedValueReplacements(insertedValuePrefix, insertedValueSuffix), () -> {
                if (discriminatorUpdatePredicate != null) {
                    builder.separator();
                    ((Ast) discriminatorUpdatePredicate).renderTo(builder);
                }
                for (Predicate predicate : updatePredicates) {
                    builder.separator();
                    ((Ast) predicate).renderTo(builder);
                }
            });
            return this;
        }

        @Override
        public InsertFromSelectContext appendReturning() {
            if (returningSelections == null) {
                throw new IllegalStateException("No returning selections are available");
            }
            renderReturningSelections(builder(), returningSelections);
            return this;
        }

        private SqlBuilder builder() {
            if (builder == null) {
                throw new IllegalStateException("The capability context cannot render SQL");
            }
            return builder;
        }

        private Map<Object, String> insertedValueReplacements(
                @Nullable String prefix,
                @Nullable String suffix
        ) {
            if (prefix == null) {
                return Collections.emptyMap();
            }
            String actualSuffix = suffix != null ? suffix : "";
            Map<Object, String> map = new HashMap<>();
            for (Assignment assignment : assignments.values()) {
                if (assignment.target.definition.size() == 1) {
                    map.put(
                            assignment.insertSource,
                            prefix + assignment.target.definition.name(0) + actualSuffix
                    );
                }
            }
            return map;
        }

        private void withSourceScope(Runnable block) {
            withSourceScope(Collections.emptyMap(), block);
        }

        private void withSourceScope(Map<Object, String> replacements, Runnable block) {
            AstContext astContext = builder().getAstContext();
            astContext.pushStatement(AbstractInsertFromSelectImpl.this);
            astContext.pushStatement(sourceQuery);
            if (!replacements.isEmpty()) {
                astContext.pushMutationExpressionMap(replacements);
            }
            try {
                block.run();
            } finally {
                if (!replacements.isEmpty()) {
                    astContext.popMutationExpressionMap();
                }
                astContext.popStatement();
                astContext.popStatement();
            }
        }
    }

    final ExecutionException unsupported(String capability) {
        return new ExecutionException(
                "Dialect \"" + getSqlClient().getDialect().getClass().getName() +
                        "\" cannot preserve " + (isUpsert() ? "upsert" : "insert") +
                        "-from-select semantics: unsupported " + capability
        );
    }

    private void acceptForAnalysis(@NotNull AstVisitor visitor) {
        AstContext astContext = visitor.getAstContext();
        astContext.pushStatement(this);
        astContext.pushStatement(sourceQuery);
        try {
            visitor.visitStatement(this);
            visitor.visitStatement(sourceQuery);
            for (Assignment assignment : assignments.values()) {
                ((Ast) assignment.target.expr).accept(visitor);
                ((Ast) assignment.insertSource).accept(visitor);
                if (assignment.updateExpression != null) {
                    ((Ast) assignment.updateExpression).accept(visitor);
                }
            }
            for (Predicate predicate : updatePredicates) {
                ((Ast) predicate).accept(visitor);
            }
            if (discriminatorUpdatePredicate != null) {
                ((Ast) discriminatorUpdatePredicate).accept(visitor);
            }
        } finally {
            astContext.popStatement();
            astContext.popStatement();
        }
    }

    @Override
    public <R> SelectionExecutable<R> returning(Selection<R> selection) {
        return returning0(Collections.singletonList(selection), null);
    }

    @Override
    public <T1, T2> SelectionExecutable<Tuple2<T1, T2>> returning(Selection<T1> s1, Selection<T2> s2) {
        return returning0(Arrays.asList(s1, s2), null);
    }

    @Override
    public <T1, T2, T3> SelectionExecutable<Tuple3<T1, T2, T3>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3
    ) {
        return returning0(Arrays.asList(s1, s2, s3), null);
    }

    @Override
    public <T1, T2, T3, T4> SelectionExecutable<Tuple4<T1, T2, T3, T4>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4), null);
    }

    @Override
    public <T1, T2, T3, T4, T5> SelectionExecutable<Tuple5<T1, T2, T3, T4, T5>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4,
            Selection<T5> s5
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4, s5), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> SelectionExecutable<Tuple6<T1, T2, T3, T4, T5, T6>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4,
            Selection<T5> s5,
            Selection<T6> s6
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4, s5, s6), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> SelectionExecutable<Tuple7<T1, T2, T3, T4, T5, T6, T7>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4,
            Selection<T5> s5,
            Selection<T6> s6,
            Selection<T7> s7
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4, s5, s6, s7), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> SelectionExecutable<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4,
            Selection<T5> s5,
            Selection<T6> s6,
            Selection<T7> s7,
            Selection<T8> s8
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8), null);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9>
    SelectionExecutable<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> returning(
            Selection<T1> s1,
            Selection<T2> s2,
            Selection<T3> s3,
            Selection<T4> s4,
            Selection<T5> s5,
            Selection<T6> s6,
            Selection<T7> s7,
            Selection<T8> s8,
            Selection<T9> s9
    ) {
        return returning0(Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8, s9), null);
    }

    @Override
    public <R> SelectionExecutable<R> returning(TupleMapper<R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return returning0(mapper.getSelections(), mapper);
    }

    private <R> SelectionExecutable<R> returning0(
            List<Selection<?>> selections,
            @Nullable TupleCreator<R> tupleCreator
    ) {
        validateMutable();
        List<Selection<?>> immutableSelections = Collections.unmodifiableList(new ArrayList<>(selections));
        return new SelectionExecutable<R>() {
            @Override
            public List<R> execute(Connection con) {
                return getSqlClient().getConnectionManager().execute(
                        con,
                        it -> executeReturning(it, immutableSelections, tupleCreator)
                );
            }

            @Override
            public Stream<R> stream(Connection con) {
                return execute(con).stream();
            }
        };
    }

    static final class Assignment {

        final Target target;
        final Expression<?> insertSource;
        final Expression<?> updateExpression;
        final Role role;

        Assignment(Target target, Expression<?> insertSource, Expression<?> updateExpression, Role role) {
            this.target = target;
            this.insertSource = insertSource;
            this.updateExpression = updateExpression;
            this.role = role;
        }
    }

    private static final class MaterializedResult<R> {

        final int affectedRowCount;

        final List<R> rows;

        MaterializedResult(int affectedRowCount, List<R> rows) {
            this.affectedRowCount = affectedRowCount;
            this.rows = rows;
        }
    }

    static final class Target {

        final Table<?> table;
        final ImmutableProp prop;
        final PropExpressionImplementor<?> expr;
        final ColumnDefinition definition;

        private Target(
                Table<?> table,
                ImmutableProp prop,
                PropExpressionImplementor<?> expr,
                ColumnDefinition definition
        ) {
            this.table = table;
            this.prop = prop;
            this.expr = expr;
            this.definition = definition;
        }

        static Target of(PropExpression<?> expression, MetadataStrategy strategy) {
            if (!(expression instanceof PropExpressionImplementor<?>)) {
                throw new IllegalArgumentException("The target must be a property expression");
            }
            PropExpressionImplementor<?> implementor = (PropExpressionImplementor<?>) expression;
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
                targetTable = parent;
            } else {
                prop = implementor.getProp();
            }
            EmbeddedColumns.Partial partial = implementor.getPartial(strategy);
            ColumnDefinition definition = partial != null ? partial : prop.getStorage(strategy);
            return new Target(targetTable, prop, implementor, definition);
        }

        Set<String> columnNames(MetadataStrategy strategy) {
            Set<String> names = new LinkedHashSet<>();
            for (String name : definition) {
                names.add(name);
            }
            return names;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Target && expr.equals(((Target) o).expr);
        }

        @Override
        public int hashCode() {
            return expr.hashCode();
        }

        @Override
        public String toString() {
            return expr.toString();
        }
    }

    private static final class NativeSqlExpression<T> implements ExpressionImplementor<T>, Ast {

        private final Class<T> type;
        private final String sql;

        @SuppressWarnings("unchecked")
        NativeSqlExpression(Class<?> type, String sql) {
            this.type = (Class<T>) type;
            this.sql = sql;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            builder.sql(sql);
        }

        @Override
        public boolean hasVirtualPredicate() {
            return false;
        }

        @Override
        public Ast resolveVirtualPredicate(AstContext ctx) {
            return this;
        }
    }

    private static class MaterializedExpression<T> extends AbstractExpression<T> {

        private final ExpressionImplementor<T> raw;

        private final Map<Object, Object> replacements;

        private MaterializedExpression(
                ExpressionImplementor<T> raw,
                Map<Object, Object> replacements
        ) {
            this.raw = raw;
            this.replacements = replacements;
        }

        @Override
        public Class<T> getType() {
            return raw.getType();
        }

        @Override
        public int precedence() {
            return raw.precedence();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            AstContext ctx = visitor.getAstContext();
            ctx.pushMutationExpressionMap(replacements);
            try {
                ((Ast) raw).accept(visitor);
            } finally {
                ctx.popMutationExpressionMap();
            }
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            builder.pushMutationExpressionMap(replacements);
            try {
                ((Ast) raw).renderTo(builder);
            } finally {
                builder.popMutationExpressionMap();
            }
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            return false;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            return this;
        }
    }

    private static final class MaterializedPredicate extends AbstractPredicate {

        private final PredicateImplementor raw;

        private final Map<Object, Object> replacements;

        private MaterializedPredicate(
                PredicateImplementor raw,
                Map<Object, Object> replacements
        ) {
            this.raw = raw;
            this.replacements = replacements;
        }

        @Override
        public int precedence() {
            return raw.precedence();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            AstContext ctx = visitor.getAstContext();
            ctx.pushMutationExpressionMap(replacements);
            try {
                ((Ast) raw).accept(visitor);
            } finally {
                ctx.popMutationExpressionMap();
            }
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            builder.pushMutationExpressionMap(replacements);
            try {
                ((Ast) raw).renderTo(builder);
            } finally {
                builder.popMutationExpressionMap();
            }
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            return false;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            return this;
        }
    }

    private final class AnalysisAst implements TypedQueryImplementor {

        @Override
        public List<Selection<?>> getSelections() {
            return Collections.emptyList();
        }

        @Override
        public JSqlClientImplementor getSqlClient() {
            return AbstractInsertFromSelectImpl.this.getSqlClient();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            acceptForAnalysis(visitor);
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            renderMutation(builder.assertSimple(), null);
        }

        @Override
        public boolean hasVirtualPredicate() {
            return false;
        }

        @Override
        public Ast resolveVirtualPredicate(AstContext ctx) {
            return this;
        }
    }
}
