package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.InheritanceType;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.TypeMatchMode;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsageCollector;
import org.babyfish.jimmer.sql.ast.impl.query.TableUsages;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
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

public class MutableUpdateImpl
        extends AbstractMutableStatementImpl
        implements MutableUpdate {

    private final StatementContext ctx;

    private final boolean triggerIgnored;

    private final Map<Target, Expression<?>> assignmentMap = new LinkedHashMap<>();

    private TableLikeImplementor<?> aliasSource;

    private TypeMatchMode typeMatchMode = TypeMatchMode.AUTO;

    private boolean typeMatchPredicateApplied;

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE);
        this.triggerIgnored = false;
    }

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType, boolean triggerIgnored) {
        super(sqlClient, immutableType);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE);
        this.triggerIgnored = triggerIgnored;
    }

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, TableProxy<?> table) {
        super(sqlClient, table);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE);
        this.triggerIgnored = false;
    }

    @Override
    public StatementContext getContext() {
        return ctx;
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
    public MutableUpdate setTypeMatchMode(TypeMatchMode mode) {
        validateMutable();
        this.typeMatchMode = mode != null ? mode : TypeMatchMode.AUTO;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> MutableUpdate set(PropExpression<X> path, X value) {
        if (value != null) {
            return set(path, Expression.any().value(value));
        }
        return set(
                path,
                Expression.nullValue(((ExpressionImplementor<X>) path).getType())
        );
    }

    @Override
    public <X> MutableUpdate set(PropExpression<X> path, Expression<X> value) {
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
        if (!TableImplementor.isPropOwnedByStage(originalProp, updateType)) {
            throw new IllegalArgumentException(
                    "Cannot update property \"" +
                    target.prop +
                    "\" by createUpdate for joined inheritance type \"" +
                    updateType +
                    "\" because it belongs to the physical table of \"" +
                    originalProp.getDeclaringType() +
                    "\""
            );
        }
    }

    @Override
    public MutableUpdate where(Predicate... predicates) {
        return (MutableUpdate) super.where(predicates);
    }

    @Override
    protected void onFrozen(AstContext astContext) {
        applyTypeMatchPredicate();
        super.onFrozen(astContext);
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

    private SqlBuilder createFilteredBuilder() {
        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
        applyVirtualPredicates(builder.getAstContext());
        applyGlobalFilters(builder.getAstContext(), FilterLevel.DEFAULT, null);
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

    void shareRootAliasWith(TableLikeImplementor<?> source) {
        this.aliasSource = source;
    }

    @Override
    public TableImplementor<?> getTableLikeImplementor() {
        return (TableImplementor<?>) super.getTableLikeImplementor();
    }

    private void accept(@NotNull AstVisitor visitor, boolean visitAssignments) {
        AstContext astContext = visitor.getAstContext();
        freeze(astContext);
        astContext.pushStatement(this);
        visitor.visitStatement(this);
        try {
            if (visitAssignments) {
                for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
                    ((Ast) e.getKey().expr).accept(visitor);
                    ((Ast) e.getValue()).accept(visitor);
                }
            }
            for (Predicate predicate : unfrozenPredicates()) {
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
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(this);
        boolean joinedTypeBranchUpdatePushed = false;
        try {
            TableImplementor<?> table = getTableLikeImplementor();
            Dialect dialect = getSqlClient().getDialect();
            VisitorImpl visitor = new VisitorImpl(builder.getAstContext(), dialect);
            this.accept(visitor);
            TableUsages tableUsages = visitor.toTableUsages();
            tableUsages.applyUsedStatesTo(astContext);
            tableUsages.allocateAndBindAliases(astContext);
            UpdateJoin updateJoin = dialect.getUpdateJoin();
            boolean joinedTypeBranchUpdate = table.isJoinedTypeBranchRoot();
            boolean joinedTypeBranchRootRequired = visitor.isJoinedTypeBranchRootRequired();
            if (joinedTypeBranchRootRequired && updateJoin == null) {
                throw new ExecutionException(
                        "Table joins for update statement is forbidden by the current dialect, " +
                        "but joined inheritance update for \"" +
                        table.getImmutableType() +
                        "\" requires joining the root table"
                );
            }
            if (joinedTypeBranchRootRequired && updateJoin.getFrom() == UpdateJoin.From.AS_ROOT) {
                throw new ExecutionException(
                        "The current dialect renders update joins from the root table, " +
                        "but joined inheritance update for \"" +
                        table.getImmutableType() +
                        "\" requires a separate root table alias"
                );
            }
            String joinedTypeBranchRootAlias = joinedTypeBranchRootRequired ?
                    astContext.getTableAliasScope().allocateTableAlias(table) :
                    null;
            if (aliasSource != null) {
                astContext.getTableAliasScope().bindAlias(
                        aliasSource.realTable(astContext),
                        getTableLikeImplementor().realTable(astContext)
                );
            }
            if (joinedTypeBranchUpdate) {
                astContext.pushJoinedTypeBranchUpdate(table, joinedTypeBranchRootAlias);
                joinedTypeBranchUpdatePushed = true;
            }
            builder
                    .sql("update ")
                    .sql(table.getImmutableType().getTableName(getSqlClient().getMetadataStrategy()));

            addAlias(builder);

            if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
                if (joinedTypeBranchRootRequired) {
                    renderJoinedTypeBranchRootJoin(builder, TableImplementor.RenderMode.NORMAL);
                }
                for (RealTable child : table.realTable(astContext)) {
                    child.renderTo(builder, false);
                }
            }

            builder.enter(SqlBuilder.ScopeType.SET);
            renderAssignments(builder);
            builder.leave();

            renderTables(builder);
            renderDeeperJoins(builder);

            renderWhereClause(builder, true, ids);

        } finally {
            if (joinedTypeBranchUpdatePushed) {
                astContext.popJoinedTypeBranchUpdate();
            }
            astContext.popStatement();
        }
    }

    private void renderAsSelect(SqlBuilder builder, Collection<Object> ids) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(this);
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
                renderWhereClause(builder, false, null);
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void renderAssignments(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean withTargetPrefix =
                updateJoin != null &&
                updateJoin.isJoinedTableUpdatable() &&
                (MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext()) ||
                        isJoinedTypeBranchRootUpdateRequired(builder));
        for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
            builder.separator();
            renderTarget(builder, e.getKey(), withTargetPrefix);
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

    private void renderTarget(SqlBuilder builder, Target target, boolean withPrefix) {
        TableImplementor<?> impl = TableProxies.resolve(target.table, builder.getAstContext());
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
            definition = target.expr.getPartial(builder.getAstContext().getSqlClient().getMetadataStrategy());
        }
        impl.renderSelection(
                target.expr.getDeepestProp(),
                true,
                builder,
                definition,
                withPrefix
        );
    }

    private void renderTables(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        boolean joinedTypeBranchRootRequired = isJoinedTypeBranchRootUpdateRequired(builder);
        boolean usedChild = MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext());
        if (joinedTypeBranchRootRequired || usedChild) {
            switch (getSqlClient().getDialect().getUpdateJoin().getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.from().enter(",");
                    if (joinedTypeBranchRootRequired) {
                        renderJoinedTypeBranchRootJoin(builder, TableImplementor.RenderMode.FROM_ONLY);
                    }
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

    private void renderWhereClause(SqlBuilder builder, boolean forUpdate, Collection<Object> ids) {

        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();

        boolean hasJoinedTypeBranchRootCondition =
                forUpdate &&
                updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                isJoinedTypeBranchRootUpdateRequired(builder);

        boolean hasTableCondition =
                forUpdate &&
                updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                MutationJoinRenderSupport.hasUsedChild(table, builder.getAstContext());

        if (!hasJoinedTypeBranchRootCondition &&
                !hasTableCondition &&
                ids == null &&
                !unfrozenPredicates().iterator().hasNext()) {
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

        if (hasJoinedTypeBranchRootCondition) {
            renderJoinedTypeBranchRootJoin(builder, TableImplementor.RenderMode.WHERE_ONLY);
        }

        if (hasTableCondition) {
            MutationJoinRenderSupport.renderUsedJoinConditions(builder, table);
        }

        if (ids == null) {
            Predicate predicate = getPredicate(builder.getAstContext());
            if (predicate != null) {
                builder.separator();
                ((Ast) predicate).renderTo(builder);
            }
        }

        builder.leave();
    }

    private boolean isJoinedTypeBranchRootUpdateRequired(SqlBuilder builder) {
        return builder.getAstContext().getJoinedTypeBranchUpdateRootAlias(getTableLikeImplementor()) != null;
    }

    private void renderJoinedTypeBranchRootJoin(
            SqlBuilder builder,
            TableImplementor.RenderMode mode
    ) {
        TableImplementor<?> table = getTableLikeImplementor();
        InheritanceInfo inheritanceInfo = table.getImmutableType().getInheritanceInfo();
        ImmutableType rootType = inheritanceInfo.getRootType();
        String rootAlias = builder.getAstContext().getJoinedTypeBranchUpdateRootAlias(table);
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        switch (mode) {
            case NORMAL:
                builder
                        .join(JoinType.INNER)
                        .sql(rootType.getTableName(strategy))
                        .sql(" ")
                        .sql(rootAlias)
                        .on();
                break;
            case FROM_ONLY:
                builder
                        .separator()
                        .sql(rootType.getTableName(strategy))
                        .sql(" ")
                        .sql(rootAlias);
                return;
            case WHERE_ONLY:
                builder.separator();
                break;
            default:
                throw new AssertionError("Internal bug: Illegal render mode for joined type branch update root");
        }
        renderJoinedTypeBranchRootJoinCondition(builder, rootAlias);
    }

    private void renderJoinedTypeBranchRootJoinCondition(SqlBuilder builder, String rootAlias) {
        TableImplementor<?> table = getTableLikeImplementor();
        MetadataStrategy strategy = getSqlClient().getMetadataStrategy();
        ImmutableType type = table.getImmutableType();
        ImmutableType rootType = type.getInheritanceInfo().getRootType();
        ColumnDefinition rootDefinition = rootType.getIdProp().getStorage(strategy);
        ColumnDefinition targetDefinition = type.getIdProp().getStorage(strategy);
        String targetAlias = MutationRender.alias(builder, table);
        int size = rootDefinition.size();
        builder.enter(SqlBuilder.ScopeType.AND);
        for (int i = 0; i < size; i++) {
            builder
                    .separator()
                    .sql(rootAlias)
                    .sql(".")
                    .sql(rootDefinition.name(i))
                    .sql(" = ")
                    .sql(targetAlias)
                    .sql(".")
                    .sql(targetDefinition.name(i));
        }
        builder.leave();
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

    private class VisitorImpl extends TableUsageCollector {

        private final Dialect dialect;

        private boolean joinedTypeBranchRootRequired;

        public VisitorImpl(AstContext astContext, Dialect dialect) {
            super(astContext);
            this.dialect = dialect;
        }

        @Override
        public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
            super.visitTableReference(table, prop, rawId);
            collectJoinedTypeBranchRootRequirement(table, prop);
            if (dialect != null) {
                validateTable(table);
            }
        }

        boolean isJoinedTypeBranchRootRequired() {
            return joinedTypeBranchRootRequired;
        }

        private void collectJoinedTypeBranchRootRequirement(RealTable table, @Nullable ImmutableProp prop) {
            TableImplementor<?> updateTable = getTableLikeImplementor();
            if (!updateTable.isJoinedTypeBranchRoot()) {
                return;
            }
            if (table.getTableLikeImplementor() == updateTable) {
                if (prop == null || !prop.isId() && updateTable.isRootTableProp(prop)) {
                    joinedTypeBranchRootRequired = true;
                    return;
                }
            }
            for (RealTable current = table; current.getParent() != null; current = current.getParent()) {
                if (current.getParent().getTableLikeImplementor() == updateTable &&
                        current.getTableLikeImplementor() instanceof TableImplementor<?>) {
                    ImmutableProp joinProp = ((TableImplementor<?>) current.getTableLikeImplementor()).getJoinProp();
                    if (joinProp != null && updateTable.isRootTableProp(joinProp)) {
                        joinedTypeBranchRootRequired = true;
                        return;
                    }
                }
            }
        }

        private void validateTable(RealTable table) {
            if (table.getTableLikeImplementor().getStatement() != MutableUpdateImpl.this) {
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
