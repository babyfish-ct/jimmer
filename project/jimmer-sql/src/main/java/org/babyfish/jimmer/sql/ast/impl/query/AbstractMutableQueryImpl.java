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
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractMutableQueryImpl
        extends AbstractMutableStatementImpl
        implements MutableQuery {

    private Table<?> table;

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
                TableImplementor.create(this, immutableType)
        );
    }

    @Override
    public AbstractMutableQueryImpl where(Predicate... predicates) {
        return (AbstractMutableQueryImpl) super.where(predicates);
    }

    @Override
    public AbstractMutableQueryImpl groupBy(Expression<?> ... expressions) {
        validateMutable();
        for (Expression<?> expression : expressions) {
            if (expression != null) {
                groupByExpressions.add(expression);
            }
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl having(Predicate ... predicates) {
        validateMutable();
        for (Predicate predicate : predicates) {
            if (predicate != null) {
                havingPredicates.add(predicate);
            }
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression) {
        validateMutable();
        return (AbstractMutableQueryImpl)MutableQuery.super.orderBy(expression);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode) {
        validateMutable();
        return (AbstractMutableQueryImpl) MutableQuery.super.orderBy(expression, orderMode);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        validateMutable();
        this.orders.add(new Order(expression, orderMode, nullOrderMode));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Table<?>> T getTable() {
        return (T)table;
    }

    @Override
    public void freeze() {
        havingPredicates = mergePredicates(havingPredicates);
        super.freeze();
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
        Predicate predicate = getPredicate();
        if (predicate != null) {
            ((Ast)predicate).accept(visitor);
        }
        for (Expression<?> expression : groupByExpressions) {
            ((Ast)expression).accept(visitor);
        }
        for (Predicate havingPredicate : havingPredicates) {
            ((Ast)havingPredicate).accept(visitor);
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

    void renderTo(SqlBuilder builder, boolean withoutSortingAndPaging) {

        Predicate predicate = getPredicate();
        Predicate havingPredicate = havingPredicates.isEmpty() ? null : havingPredicates.get(0);

        TableImplementor<?> table = TableImplementor.unwrap(this.table);
        table.renderTo(builder);

        if (predicate != null) {
            builder.sql(" where ");
            ((Ast)predicate).renderTo(builder);
        }
        if (!groupByExpressions.isEmpty()) {
            String separator = " group by ";
            for (Expression<?> expression : groupByExpressions) {
                builder.sql(separator);
                ((Ast)expression).renderTo(builder);
                separator = ", ";
            }
        }
        if (havingPredicate != null) {
            builder.sql(" having ");
            ((Ast)havingPredicate).renderTo(builder);
        }
        if (!withoutSortingAndPaging && !orders.isEmpty()) {
            String separator = " order by ";
            for (Order order : orders) {
                builder.sql(separator);
                ((Ast)order.expression).renderTo(builder);
                if (order.orderMode == OrderMode.ASC) {
                    builder.sql(" asc");
                } else {
                    builder.sql(" desc");
                }
                if (order.nullOrderMode == NullOrderMode.NULLS_FIRST) {
                    builder.sql(" nulls first");
                } else if (order.nullOrderMode == NullOrderMode.NULLS_LAST) {
                    builder.sql(" nulls last");
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
