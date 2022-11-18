package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.StatementContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
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

    public MutableUpdateImpl(JSqlClient sqlClient, ImmutableType immutableType) {
        super(sqlClient, immutableType);
        this.ctx = new StatementContext(ExecutionPurpose.UPDATE, false);
    }

    public MutableUpdateImpl(JSqlClient sqlClient, TableProxy<?> table) {
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
        Target target = Target.of(path);
        if (target.table != this.getTable() && getSqlClient().getTriggerType() != TriggerType.BINLOG_ONLY) {
            throw new IllegalArgumentException(
                    "Only the primary table can be deleted when transaction trigger is supported"
            );
        }
        if (!(target.prop.getStorage() instanceof Column)) {
            throw new IllegalArgumentException("The assigned prop expression must be mapped as column");
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
        Tuple2<String, List<Object>> sqlResult = builder.build();
        return getSqlClient()
                .getExecutor()
                .execute(
                        con,
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        getPurpose(),
                        null,
                        PreparedStatement::executeUpdate
                );
    }

    private int executeWithTrigger(Connection con) {

        SqlBuilder builder = new SqlBuilder(new AstContext(getSqlClient()));
        renderAsSelect(builder, null);
        Tuple2<String, List<Object>> sqlResult = builder.build();
        List<ImmutableSpi> rows = Selectors.select(
                getSqlClient(),
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                Collections.singletonList(this.getTable()),
                ExecutionPurpose.UPDATE
        );
        if (rows.isEmpty()) {
            return 0;
        }

        int idPropId = getTable().getImmutableType().getIdProp().getId();
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
                        con,
                        sqlResult.get_1(),
                        sqlResult.get_2(),
                        getPurpose(),
                        null,
                        PreparedStatement::executeUpdate
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
                Collections.singletonList(this.getTable()),
                ExecutionPurpose.UPDATE
        );
        MutationTrigger trigger = new MutationTrigger();
        for (ImmutableSpi changedRow : changedRows) {
            ImmutableSpi row = rowMap.get(changedRow.__get(idPropId));
            if (!row.__equals(changedRow, true)) {
                trigger.modifyEntityTable(row, changedRow);
            } else {
                System.out.println(changedRow + " -> " + row);
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
                    .sql(table.getImmutableType().getTableName())
                    .sql(" ")
                    .sql(table.getAlias());

            UpdateJoin updateJoin = dialect.getUpdateJoin();
            if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
                for (TableImplementor<?> child : table) {
                    child.renderTo(builder);
                }
            }
            builder.sql(" set ");
            renderAssignments(builder);
            renderTables(builder);
            renderDeeperJoins(builder);
            renderPredicates(builder, true, ids);
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
            builder.sql("select ");
            boolean addComma = false;
            for (ImmutableProp prop : table.getImmutableType().getSelectableProps().values()) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.sql(table.getAlias()).sql(".").sql(prop.<Column>getStorage().getName());
            }
            if (ids != null) {
                builder
                        .sql(" from ")
                        .sql(table.getImmutableType().getTableName())
                        .sql(" as ")
                        .sql(table.getAlias());
                builder
                        .sql(" where ")
                        .sql(table.getAlias())
                        .sql(".")
                        .sql(table.getImmutableType().getIdProp().<Column>getStorage().getName())
                        .sql(" in(");
                addComma = false;
                for (Object id : ids) {
                    if (addComma) {
                        builder.sql(", ");
                    } else {
                        addComma = true;
                    }
                    builder.variable(id);
                }
                builder.sql(")");
            } else {
                table.renderTo(builder);
                renderPredicates(builder, false, null);
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
        String separator = "";
        for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
            builder.sql(separator);
            renderTarget(builder, e.getKey(), withTargetPrefix);
            builder.sql(" = ");
            ((Ast) e.getValue()).renderTo(builder);
            separator = ", ";
        }
    }

    private void renderTarget(SqlBuilder builder, Target target, boolean withPrefix) {
        if (withPrefix) {
            TableImplementor<?> impl = TableProxies.resolve(target.table, builder.getAstContext());
            builder.sql(impl.getAlias()).sql(".");
        }
        builder.sql(((Column) target.prop.getStorage()).getName());
    }

    private void renderTables(SqlBuilder builder) {
        TableImplementor<?> table = getTableImplementor();
        if (hasUsedChild(table, builder.getAstContext())) {
            switch (getSqlClient().getDialect().getUpdateJoin().getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.sql(" from ");
                    String separator = "";
                    for (TableImplementor<?> child : table) {
                        builder.sql(separator);
                        child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
                        separator = ",";
                    }
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

    private void renderPredicates(SqlBuilder builder, boolean forUpdate, Collection<Object> ids) {
        TableImplementor<?> table = getTableImplementor();
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        String separator = " where ";
        if (ids != null) {
            ImmutableProp idProp = table.getImmutableType().getIdProp();
            builder
                    .sql(separator)
                    .sql(table.getAlias())
                    .sql(".")
                    .sql(idProp.<Column>getStorage().getName())
                    .sql(" in(");
            boolean addComma = false;
            for (Object id : ids) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                builder.variable(id);
            }
            builder.sql(")");
            separator = " and ";
        }
        if (forUpdate &&
                updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                hasUsedChild(table, builder.getAstContext())
        ) {
            for (TableImplementor<?> child : table) {
                builder.sql(separator);
                separator = " and ";
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
            }
        }
        Predicate predicate = getPredicate();
        if (predicate != null) {
            builder.sql(separator);
            ((Ast)predicate).renderTo(builder);
        }
    }

    private static class Target {

        Table<?> table;

        ImmutableProp prop;

        PropExpression<?> expr;

        private Target(Table<?> table, ImmutableProp prop, PropExpression<?> expr) {
            this.table = table;
            this.prop = prop;
            this.expr = expr;
        }

        static Target of(PropExpression<?> expr) {
            PropExpressionImpl<?> exprImpl = (PropExpressionImpl<?>) expr;
            Table<?> targetTable = exprImpl.getTable();
            Table<?> parent;
            ImmutableProp prop;
            if (targetTable instanceof TableImplementor<?>) {
                parent = ((TableImplementor<?>)targetTable).getParent();
                prop = ((TableImplementor<?>)targetTable).getJoinProp();
            } else {
                parent = ((TableProxy<?>)targetTable).__parent();
                prop = ((TableProxy<?>)targetTable).__prop();
            }
            if (parent != null && exprImpl.getProp().isId()) {
                return new Target(parent, prop, expr);
            } else {
                return new Target(targetTable, exprImpl.getProp(), expr);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;
            return table.equals(target.table) && prop.equals(target.prop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(table, prop);
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
