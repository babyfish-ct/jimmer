package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class MutableUpdateImpl
        extends AbstractMutableStatementImpl
        implements MutableUpdate, Ast {

    private final StatementContext ctx;

    private Map<Target, Expression<?>> assignmentMap = new LinkedHashMap<>();

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE, false);
    }

    public MutableUpdateImpl(JSqlClientImplementor sqlClient, TableProxy<?> table) {
        super(sqlClient, table);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE, false);
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
                Expression.any().nullValue(((ExpressionImplementor<X>)path).getType())
        );
    }

    @Override
    public <X> MutableUpdate set(PropExpression<X> path, Expression<X> value) {
        validateMutable();
        Target target = Target.of(path, getSqlClient().getMetadataStrategy());
        if (target.table != this.getTable() && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            throw new IllegalArgumentException(
                    "Only the primary table can be deleted when transaction trigger is supported"
            );
        }
        if (!(target.prop.isColumnDefinition())) {
            throw new IllegalArgumentException("The assigned prop expression must be mapped by database columns");
        }
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean joinedTableUpdatable = updateJoin != null && updateJoin.isJoinedTableUpdatable();
        if (!joinedTableUpdatable && (target.table != getTable() && target.table != getTableImplementor())) {
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
    public MutableUpdate where(Predicate ... predicates) {
        return (MutableUpdate) super.where(predicates);
    }

    @Override
    public Integer execute() {
        return getSqlClient()
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public Integer execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return getSqlClient()
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    private int executeImpl(Connection con) {
        freeze();
        if (assignmentMap.isEmpty()) {
            return 0;
        }

        if (getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            return executeWithTrigger(con);
        }

        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
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
                                PreparedStatement::executeUpdate
                        )
                );
    }

    private int executeWithTrigger(Connection con) {

        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
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

        PropId idPropId = getTable().getImmutableType().getIdProp().getId();
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
                                PreparedStatement::executeUpdate
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

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        accept(visitor, true);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderTo(builder, null);
    }

    private void accept(@NotNull AstVisitor visitor, boolean visitAssignments) {
        AstContext astContext = visitor.getAstContext();
        astContext.pushStatement(this);
        try {
            if (visitAssignments) {
                for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
                    ((Ast) e.getKey().expr).accept(visitor);
                    ((Ast) e.getValue()).accept(visitor);
                }
            }
            Predicate predicate = getPredicate();
            if (predicate != null) {
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
            TableImplementor<?> table = getTableImplementor();
            Dialect dialect = getSqlClient().getDialect();
            this.accept(new VisitorImpl(builder.getAstContext(), dialect));
            builder
                    .sql("update ")
                    .sql(table.getImmutableType().getTableName(getSqlClient().getMetadataStrategy()))
                    .sql(" ")
                    .sql(table.getAlias());

            UpdateJoin updateJoin = dialect.getUpdateJoin();
            if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
                for (TableImplementor<?> child : table) {
                    child.renderTo(builder);
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
            accept(new VisitorImpl(builder.getAstContext(), null), false);
            TableImplementor<?> table = getTableImplementor();
            MetadataStrategy strategy = builder.getAstContext().getSqlClient().getMetadataStrategy();
            builder.enter(SqlBuilder.ScopeType.SELECT);
            for (ImmutableProp prop : table.getImmutableType().getSelectableProps().values()) {
                builder.separator().definition(table.getAlias(), prop.getStorage(strategy));
            }
            builder.leave();
            if (ids != null) {
                builder
                        .from()
                        .sql(table.getImmutableType().getTableName(strategy))
                        .sql(" ")
                        .sql(table.getAlias())
                        .enter(SqlBuilder.ScopeType.WHERE)
                        .definition(table.getAlias(), table.getImmutableType().getIdProp().getStorage(strategy), true)
                        .sql(" in ").enter(SqlBuilder.ScopeType.LIST);
                for (Object id : ids) {
                    builder.separator().variable(id);
                }
                builder.leave().leave();
            } else {
                table.renderTo(builder);
                renderWhereClause(builder, false, null);
            }
        } finally {
            astContext.popStatement();
        }
    }

    private void renderAssignments(SqlBuilder builder) {
        TableImplementor<?> table = getTableImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean withTargetPrefix =
                updateJoin != null &&
                        updateJoin.isJoinedTableUpdatable() &&
                        hasUsedChild(table, builder.getAstContext());
        for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
            builder.separator();
            renderTarget(builder, e.getKey(), withTargetPrefix);
            builder.sql(" = ");
            ((Ast) e.getValue()).renderTo(builder);
        }
    }

    private void renderTarget(SqlBuilder builder, Target target, boolean withPrefix) {
        TableImplementor<?> impl = TableProxies.resolve(target.table, builder.getAstContext());
        impl.renderSelection(
                target.prop,
                builder,
                target.expr.getPartial(builder.getAstContext().getSqlClient().getMetadataStrategy()),
                withPrefix
        );
    }

    private void renderTables(SqlBuilder builder) {
        TableImplementor<?> table = getTableImplementor();
        if (hasUsedChild(table, builder.getAstContext())) {
            switch (getSqlClient().getDialect().getUpdateJoin().getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.from().enter(",");
                    for (TableImplementor<?> child : table) {
                        builder.separator();
                        child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
                    }
                    builder.leave();
            }
        }
    }

    private void renderDeeperJoins(SqlBuilder builder) {
        TableImplementor<?> table = getTableImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        if (updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                hasUsedChild(table, builder.getAstContext())
        ) {
            for (TableImplementor<?> child : table) {
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.DEEPER_JOIN_ONLY);
            }
        }
    }

    private void renderWhereClause(SqlBuilder builder, boolean forUpdate, Collection<Object> ids) {

        TableImplementor<?> table = getTableImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();

        boolean hasTableCondition =
                forUpdate &&
                        updateJoin != null &&
                        updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                        hasUsedChild(table, builder.getAstContext());

        if (!hasTableCondition && ids == null && getPredicate() == null) {
            return;
        }

        builder.enter(SqlBuilder.ScopeType.WHERE);

        if (ids != null) {
            ImmutableProp idProp = table.getImmutableType().getIdProp();
            builder
                    .separator()
                    .definition(table.getAlias(), idProp.getStorage(getSqlClient().getMetadataStrategy()), true)
                    .sql(" in ")
                    .enter(SqlBuilder.ScopeType.LIST);
            for (Object id : ids) {
                builder.separator().variable(id);
            }
            builder.leave();
        }

        if (hasTableCondition) {
            for (TableImplementor<?> child : table) {
                builder.separator();
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
            }
        }

        if (ids == null) {
            Predicate predicate = getPredicate();
            if (predicate != null) {
                builder.separator();
                ((Ast) predicate).renderTo(builder);
            }
        }

        builder.leave();
    }

    private static class Target {

        Table<?> table;

        ImmutableProp prop;

        PropExpressionImplementor<?> expr;

        private Target(Table<?> table, ImmutableProp prop, PropExpression<?> expr) {
            this.table = table;
            this.prop = prop;
            this.expr = (PropExpressionImplementor<?>)expr;
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
                parent = ((TableImplementor<?>)targetTable).getParent();
                prop = ((TableImplementor<?>)targetTable).getJoinProp();
            } else {
                parent = ((TableProxy<?>)targetTable).__parent();
                prop = ((TableProxy<?>)targetTable).__prop();
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
        public void visitTableReference(TableImplementor<?> table, ImmutableProp prop) {
            super.visitTableReference(table, prop);
            if (dialect != null) {
                validateTable(table);
            }
        }

        private void validateTable(TableImplementor<?> tableImpl) {
            if (getAstContext().getTableUsedState(tableImpl) == TableUsedState.USED) {
                if (tableImpl.getParent() != null && dialect.getUpdateJoin() == null) {
                    throw new ExecutionException(
                            "Table joins for update statement is forbidden by the current dialect, " +
                                    "but there is a join '" +
                                    tableImpl +
                                    "'."
                    );
                }
                if (tableImpl.getParent() != null &&
                        tableImpl.getParent().getParent() == null &&
                        tableImpl.getJoinType() != JoinType.INNER &&
                        dialect.getUpdateJoin() != null &&
                        dialect.getUpdateJoin().getFrom() == UpdateJoin.From.AS_JOIN) {
                    throw new ExecutionException(
                            "The first level table joins cannot be outer join " +
                                    "because current dialect '" +
                                    dialect.getClass().getName() +
                                    "' " +
                                    "indicates that the first level table joins in update statement " +
                                    "must be rendered as 'from' clause, " +
                                    "but there is a first level table join whose join type is outer: '" +
                                    tableImpl +
                                    "'."
                    );
                }
            }
            if (tableImpl.getParent() != null) {
                validateTable(tableImpl.getParent());
            }
        }
    }

    private static boolean hasUsedChild(TableImplementor<?> tableImplementor, AstContext astContext) {
        for (TableImplementor<?> child : tableImplementor) {
            if (astContext.getTableUsedState(child) == TableUsedState.USED) {
                return true;
            }
        }
        return false;
    }
}
