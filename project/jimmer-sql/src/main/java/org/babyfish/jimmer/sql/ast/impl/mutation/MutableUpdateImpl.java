package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.FilterLevel;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
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

    private static boolean hasUsedChild(TableImplementor<?> tableImplementor, AstContext astContext) {
        for (RealTable childTable : tableImplementor.realTable(astContext)) {
            if (astContext.getTableUsedState(childTable) == TableUsedState.USED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StatementContext getContext() {
        return ctx;
    }

    @Override
    public AbstractMutableStatementImpl getParent() {
        return null;
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

    @Override
    public MutableUpdate where(Predicate... predicates) {
        return (MutableUpdate) super.where(predicates);
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

        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
        applyVirtualPredicates(builder.getAstContext());
        applyGlobalFilters(builder.getAstContext(), FilterLevel.DEFAULT, null);

        if (!triggerIgnored && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            return executeWithTrigger(builder, con);
        }

        renderTo(builder);
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

        renderAsSelect(builder, null);
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        List<ImmutableSpi> rows = Selectors.select(
                getSqlClient(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Collections.singletonList(this.getTable()),
                ExecutionPurpose.UPDATE
        );
        if (rows.isEmpty()) {
            return 0;
        }

        PropId idPropId = this.<Table<?>>getTable().getImmutableType().getIdProp().getId();
        Map<Object, ImmutableSpi> rowMap = new HashMap<>((rows.size() * 4 + 2) / 3);
        for (ImmutableSpi row : rows) {
            rowMap.put(row.__get(idPropId), row);
        }

        builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderTo(builder, rowMap.keySet());
        sqlResult = builder.build();
        int affectRowCount = getSqlClient()
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
        if (affectRowCount == 0) {
            return 0;
        }

        builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderAsSelect(builder, rowMap.keySet());
        sqlResult = builder.build();
        List<ImmutableSpi> changedRows = Selectors.select(
                getSqlClient(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                Collections.singletonList(this.getTable()),
                ExecutionPurpose.UPDATE
        );
        MutationTrigger trigger = new MutationTrigger();
        for (ImmutableSpi changedRow : changedRows) {
            ImmutableSpi row = rowMap.get(changedRow.__get(idPropId));
            if (!row.__equals(changedRow, true)) {
                trigger.modifyEntityTable(row, changedRow);
            }
        }
        trigger.submit(getSqlClient(), con);
        return affectRowCount;
    }

    public void accept(@NotNull AstVisitor visitor) {
        accept(visitor, true);
    }

    public void renderTo(@NotNull SqlBuilder builder) {
        renderTo(builder, null);
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

    private void renderTo(@NotNull SqlBuilder builder, Collection<Object> ids) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(this);
        try {
            TableImplementor<?> table = getTableLikeImplementor();
            Dialect dialect = getSqlClient().getDialect();
            VisitorImpl visitor = new VisitorImpl(builder.getAstContext(), dialect);
            this.accept(visitor);
            visitor.allocateAliases();
            builder
                    .sql("update ")
                    .sql(table.getImmutableType().getTableName(getSqlClient().getMetadataStrategy()));

            if (getSqlClient().getDialect().isUpdateAliasSupported()) {
                builder.sql(" ").sql(table.realTable(builder.getAstContext()).getAlias());
            }

            UpdateJoin updateJoin = dialect.getUpdateJoin();
            if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
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
            astContext.popStatement();
        }
    }

    private void renderAsSelect(SqlBuilder builder, Collection<Object> ids) {
        AstContext astContext = builder.getAstContext();
        astContext.pushStatement(this);
        try {
            VisitorImpl visitor = new VisitorImpl(builder.getAstContext(), null);
            accept(visitor, false);
            visitor.allocateAliases();
            TableImplementor<?> table = getTableLikeImplementor();
            MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
            builder.enter(SqlBuilder.ScopeType.SELECT);
            for (ImmutableProp prop : table.getImmutableType().getSelectableProps().values()) {
                builder.separator().definition(
                        table.realTable(astContext).getAlias(),
                        prop.getStorage(strategy),
                        null
                );
            }
            builder.leave();
            if (ids != null) {
                builder
                        .from()
                        .sql(table.getImmutableType().getTableName(strategy))
                        .sql(" ");

                if (getSqlClient().getDialect().isUpdateAliasSupported()) {
                    builder.sql(table.realTable(astContext).getAlias());
                }

                builder.enter(SqlBuilder.ScopeType.WHERE);
                NativePredicates.renderPredicates(
                        false,
                        table.realTable(astContext).getAlias(),
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
                hasUsedChild(table, builder.getAstContext());
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
        if (hasUsedChild(table, builder.getAstContext())) {
            switch (getSqlClient().getDialect().getUpdateJoin().getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.from().enter(",");
                    for (RealTable child : table.realTable(builder.getAstContext())) {
                        child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
                    }
                    builder.leave();
            }
        }
    }

    private void renderDeeperJoins(SqlBuilder builder) {
        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        if (updateJoin != null &&
            updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
            hasUsedChild(table, builder.getAstContext())
        ) {
            for (RealTable child : table.realTable(builder.getAstContext())) {
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.DEEPER_JOIN_ONLY);
            }
        }
    }

    private void renderWhereClause(SqlBuilder builder, boolean forUpdate, Collection<Object> ids) {

        TableImplementor<?> table = getTableLikeImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();

        boolean hasTableCondition =
                forUpdate &&
                updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                hasUsedChild(table, builder.getAstContext());

        if (!hasTableCondition && ids == null && !unfrozenPredicates().iterator().hasNext()) {
            return;
        }

        builder.enter(SqlBuilder.ScopeType.WHERE);
        if (ids != null) {
            NativePredicates.renderPredicates(
                    false,
                    table.realTable(builder.getAstContext()).getAlias(),
                    table.getImmutableType().getIdProp().getStorage(getSqlClient().getMetadataStrategy()),
                    ids,
                    builder
            );
        }

        if (hasTableCondition) {
            for (RealTable child : table.realTable(builder.getAstContext())) {
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
            }
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

    private static class VisitorImpl extends UseTableVisitor {

        private final Dialect dialect;

        public VisitorImpl(AstContext astContext, Dialect dialect) {
            super(astContext);
            this.dialect = dialect;
        }

        @Override
        public void visitTableReference(RealTable table, @Nullable ImmutableProp prop, boolean rawId) {
            super.visitTableReference(table, prop, rawId);
            if (dialect != null) {
                validateTable(table);
            }
        }

        private void validateTable(RealTable table) {
            if (getAstContext().getTableUsedState(table) == TableUsedState.USED) {
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
