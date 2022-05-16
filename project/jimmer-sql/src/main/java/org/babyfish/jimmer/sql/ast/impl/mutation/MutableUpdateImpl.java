package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.query.UseTableVisitor;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.UpdateJoin;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import javax.persistence.criteria.JoinType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class MutableUpdateImpl
        extends AbstractMutableStatementImpl
        implements MutableUpdate, Executable<Integer>, Ast {

    private Map<Target, Expression<?>> assignmentMap = new LinkedHashMap<>();

    private List<Predicate> predicates = new ArrayList<>();

    private Table<?> table;

    public MutableUpdateImpl(SqlClient sqlClient, ImmutableType immutableType) {
        super(new TableAliasAllocator(), sqlClient);
        table = TableWrappers.wrap(
                TableImplementor.create(this, immutableType)
        );
    }

    @SuppressWarnings("unchecked")
    public <T extends Table<?>> T getTable() {
        return (T)table;
    }

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
        if (!(target.prop.getStorage() instanceof Column)) {
            throw new IllegalArgumentException("The assigned prop expression must be mapped as column");
        }
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean joinedTableUpdatable = updateJoin != null && updateJoin.isJoinedTableUpdatable();
        if (!joinedTableUpdatable && target.tableImpl != TableImplementor.unwrap(table)) {
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
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                this.predicates.add(predicate);
            }
        }
        return null;
    }

    @Override
    public Integer execute(Connection con) {
        if (assignmentMap.isEmpty()) {
            return 0;
        }
        SqlBuilder builder = new SqlBuilder(getSqlClient());
        renderTo(builder);
        Tuple2<String, List<Object>> sqlResult = builder.build();
        return getSqlClient()
                .getExecutor()
                .execute(con, sqlResult._1(), sqlResult._2(), PreparedStatement::executeUpdate);
    }

    @Override
    public void accept(AstVisitor visitor) {
        for (Map.Entry<Target, Expression<?>> e : assignmentMap.entrySet()) {
            ((Ast) e.getKey().expr).accept(visitor);
            ((Ast) e.getValue()).accept(visitor);
        }
        for (Predicate predicate : predicates) {
            ((Ast) predicate).accept(visitor);
        }
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        Dialect dialect = getSqlClient().getDialect();
        this.accept(new VisitorImpl(builder, dialect));
        builder
                .sql("update ")
                .sql(table.getImmutableType().getTableName())
                .sql(" ")
                .sql(table.getAlias());

        UpdateJoin updateJoin = dialect.getUpdateJoin();
        if (updateJoin != null && updateJoin.getFrom() == UpdateJoin.From.UNNECESSARY) {
            for (TableImplementor<?> child : table.getChildren()) {
                child.renderTo(builder);
            }
        }
        builder.sql(" set ");
        renderAssignments(builder);
        renderTables(builder);
        renderDeeperJoins(builder);
        renderPredicates(builder);
    }

    private void renderAssignments(SqlBuilder builder) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        boolean withTargetPrefix =
                updateJoin != null &&
                        updateJoin.isJoinedTableUpdatable() &&
                        table.getChildren().stream().anyMatch(it -> builder.isTableUsed(it));
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
            builder.sql(target.tableImpl.getAlias()).sql(".");
        }
        builder.sql(((Column) target.prop.getStorage()).getName());
    }

    private void renderTables(SqlBuilder builder) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        if (table.getChildren().stream().anyMatch(it -> builder.isTableUsed(it))) {
            switch (getSqlClient().getDialect().getUpdateJoin().getFrom()) {
                case AS_ROOT:
                    table.renderTo(builder);
                    break;
                case AS_JOIN:
                    builder.sql(" from ");
                    String separator = "";
                    for (TableImplementor<?> child : table.getChildren()) {
                        builder.sql(separator);
                        child.renderJoinAsFrom(builder, TableImplementor.RenderMode.FROM_ONLY);
                        separator = ",";
                    }
            }
        }
    }

    private void renderDeeperJoins(SqlBuilder builder) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        if (updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                table.getChildren().stream().anyMatch(it -> builder.isTableUsed(it))
        ) {
            for (TableImplementor<?> child : table.getChildren()) {
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.DEEPER_JOIN_ONLY);
            }
        }
    }

    private void renderPredicates(SqlBuilder builder) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        UpdateJoin updateJoin = getSqlClient().getDialect().getUpdateJoin();
        String separator = " where ";
        if (updateJoin != null &&
                updateJoin.getFrom() == UpdateJoin.From.AS_JOIN &&
                table.getChildren().stream().anyMatch(builder::isTableUsed)
        ) {
            for (TableImplementor<?> child : table.getChildren()) {
                builder.sql(separator);
                separator = " and ";
                child.renderJoinAsFrom(builder, TableImplementor.RenderMode.WHERE_ONLY);
            }
        }
        for (Predicate predicate : predicates) {
            builder.sql(separator);
            separator = " and ";
            ((Ast) predicate).renderTo(builder);
        }
    }

    private static class Target {

        TableImplementor<?> tableImpl;

        ImmutableProp prop;

        PropExpression<?> expr;

        private Target(TableImplementor<?> tableImpl, ImmutableProp prop, PropExpression<?> expr) {
            this.tableImpl = tableImpl;
            this.prop = prop;
            this.expr = expr;
        }

        static Target of(PropExpression<?> expr) {
            PropExpressionImpl<?> exprImpl = (PropExpressionImpl<?>) expr;
            TableImplementor<?> targetTable = TableImplementor.unwrap(exprImpl.getTableImplementor());
            if (targetTable.getParent() != null && exprImpl.getProp().isId()) {
                return new Target(targetTable.getParent(), targetTable.getJoinProp(), expr);
            } else {
                return new Target(targetTable, exprImpl.getProp(), expr);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;
            return tableImpl.equals(target.tableImpl) && prop.equals(target.prop);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableImpl, prop);
        }
    }

    private static class VisitorImpl extends UseTableVisitor {

        private Dialect dialect;

        public VisitorImpl(SqlBuilder sqlBuilder, Dialect dialect) {
            super(sqlBuilder);
            this.dialect = dialect;
        }

        @Override
        public void visitTableReference(Table<?> table, ImmutableProp prop) {
            super.visitTableReference(table, prop);
            validateTable(TableImplementor.unwrap(table));
        }

        private void validateTable(TableImplementor<?> tableImpl) {
            if (getSqlBuilder().isTableUsed(tableImpl)) {
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
}
