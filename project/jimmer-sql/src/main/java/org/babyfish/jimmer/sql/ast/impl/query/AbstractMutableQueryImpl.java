package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.table.TableAliasAllocator;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableRowCountDestructive;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class AbstractMutableQueryImpl
        extends AbstractMutableStatementImpl
        implements MutableQuery {

    private Table<?> table;

    private List<Predicate> predicates = new ArrayList<>();

    private List<Expression<?>> groupByExpressions = new ArrayList<>();

    private List<Predicate> havingPredicates = new ArrayList<>();

    private List<Order> orders = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected AbstractMutableQueryImpl(
            TableAliasAllocator tableAliasAllocator,
            SqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(tableAliasAllocator, sqlClient);
        this.table = TableWrappers.wrap(
                createTableImpl(immutableType)
        );
    }

    @Override
    public AbstractMutableQueryImpl where(Predicate ... predicates) {
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                this.predicates.add(predicate);
            }
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl groupBy(Expression<?> ... expressions) {
        for (Expression<?> expression : expressions) {
            if (expression != null) {
                groupByExpressions.add(expression);
            }
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl having(Predicate ... predicates) {
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                havingPredicates.add(predicate);
            }
        }
        return this;
    }

    protected TableImplementor<?> createTableImpl(ImmutableType immutableType) {
        return TableImplementor.create(this, immutableType);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression) {
        return (AbstractMutableQueryImpl)MutableQuery.super.orderBy(expression);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode) {
        return (AbstractMutableQueryImpl) MutableQuery.super.orderBy(expression, orderMode);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        this.orders.add(new Order(expression, orderMode, nullOrderMode));
        return this;
    }

    @Override
    public <T extends TableEx<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<T> tableType, BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    ) {
        return Queries.createSubQuery(this, tableType, block);
    }

    @Override
    public <T extends TableEx<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType, BiConsumer<MutableSubQuery, T> block
    ) {
        return Queries.createWildSubQuery(this, tableType, block);
    }

    Table<?> getTable() {
        return table;
    }

    void accept(
            AstVisitor visitor,
            List<Selection<?>> overriddenSelections,
            boolean withoutSortingAndPaging
    ) {
        if (groupByExpressions.isEmpty() && !havingPredicates.isEmpty()) {
            throw new IllegalStateException(
                    "Having clause cannot be used without group clause"
            );
        }
        for (Predicate predicate : predicates) {
            ((Ast)predicate).accept(visitor);
        }
        for (Expression<?> expression : groupByExpressions) {
            ((Ast)expression).accept(visitor);
        }
        for (Predicate predicate : havingPredicates) {
            ((Ast)predicate).accept(visitor);
        }
        if (withoutSortingAndPaging) {
            AstVisitor ignoredVisitor = new UseJoinOfIgnoredClauseVisitor(visitor.getSqlBuilder());
            for (Order order : orders) {
                ((Ast)order.expression).accept(ignoredVisitor);
            }
        } else {
            for (Order order : orders) {
                ((Ast)order.expression).accept(visitor);
            }
        }
        if (overriddenSelections != null) {
            AstVisitor ignoredVisitor = new UseJoinOfIgnoredClauseVisitor(visitor.getSqlBuilder());
            for (Selection<?> selection : overriddenSelections) {
                Ast.from(selection).accept(ignoredVisitor);
            }
        }
    }

    void renderTo(SqlBuilder sqlBuilder, boolean withoutSortingAndPaging) {
        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        table.renderTo(sqlBuilder);
        if (!predicates.isEmpty()) {
            String separator = " where ";
            for (Predicate predicate : predicates) {
                sqlBuilder.sql(separator);
                ((Ast)predicate).renderTo(sqlBuilder);
                separator = " and ";
            }
        }
        if (!groupByExpressions.isEmpty()) {
            String separator = " group by ";
            for (Expression<?> expression : groupByExpressions) {
                sqlBuilder.sql(separator);
                ((Ast)expression).renderTo(sqlBuilder);
                separator = ", ";
            }
        }
        if (!havingPredicates.isEmpty()) {
            String separator = " having ";
            for (Predicate predicate : havingPredicates) {
                sqlBuilder.sql(separator);
                ((Ast)predicate).renderTo(sqlBuilder);
                separator = " and ";
            }
        }
        if (!withoutSortingAndPaging && !orders.isEmpty()) {
            String separator = " order by ";
            for (Order order : orders) {
                sqlBuilder.sql(separator);
                ((Ast)order.expression).renderTo(sqlBuilder);
                if (order.orderMode == OrderMode.ASC) {
                    sqlBuilder.sql(" asc");
                } else {
                    sqlBuilder.sql(" desc");
                }
                if (order.nullOrderMode == NullOrderMode.NULLS_FIRST) {
                    sqlBuilder.sql(" nulls first");
                } else if (order.nullOrderMode == NullOrderMode.NULLS_LAST) {
                    sqlBuilder.sql(" nulls last");
                }
                separator = ", ";
            }
        }
    }

    protected boolean isGroupByClauseUsed() {
        return !this.groupByExpressions.isEmpty();
    }

    private static class Order {
        Expression<?> expression;
        OrderMode orderMode;
        NullOrderMode nullOrderMode;

        public Order(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
            this.expression = expression;
            this.orderMode = orderMode;
            this.nullOrderMode = nullOrderMode;
        }
    }

    private static class UseJoinOfIgnoredClauseVisitor extends AstVisitor {

        public UseJoinOfIgnoredClauseVisitor(SqlBuilder sqlBuilder) {
            super(sqlBuilder);
        }

        @Override
        public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
            return false;
        }

        @Override
        public void visitTableReference(Table<?> table, ImmutableProp prop) {
            handle(TableImplementor.unwrap(table), prop != null && prop.isId());
        }

        private void handle(TableImplementor<?> table, boolean isId) {
            if (table.getDestructive() != TableRowCountDestructive.NONE) {
                if (isId) {
                    use(table.getParent());
                } else {
                    use(table);
                }
            }
        }

        private void use(TableImplementor<?> table) {
            if (table != null) {
                getSqlBuilder().useTable(table);
                use(table.getParent());
            }
        }
    }
}
