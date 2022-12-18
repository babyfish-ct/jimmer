package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableRowCountDestructive;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractMutableQueryImpl
        extends AbstractMutableStatementImpl
        implements MutableQuery, SortableImplementor {

    private final List<Expression<?>> groupByExpressions = new ArrayList<>();

    private List<Predicate> havingPredicates = new ArrayList<>();

    private final List<Order> orders = new ArrayList<>();

    private int subQueryDisabledCount = 0;

    @SuppressWarnings("unchecked")
    protected AbstractMutableQueryImpl(
            JSqlClient sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
    }

    protected AbstractMutableQueryImpl(
            JSqlClient sqlClient,
            TableProxy<?> table
    ) {
        super(sqlClient, table);
    }

    @Override
    public AbstractMutableQueryImpl where(Predicate... predicates) {
        return (AbstractMutableQueryImpl) super.where(predicates);
    }

    @OldChain
    @Override
    public AbstractMutableQueryImpl whereIf(boolean condition, Predicate predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }

    @OldChain
    @Override
    public AbstractMutableQueryImpl whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
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
    public AbstractMutableQueryImpl orderBy(Expression<?> ... expressions) {
        validateMutable();
        return (AbstractMutableQueryImpl)MutableQuery.super.orderBy(expressions);
    }

    @Override
    public AbstractMutableQueryImpl orderByIf(boolean condition, Expression<?>... expressions) {
        return (AbstractMutableQueryImpl) MutableQuery.super.orderByIf(condition, expressions);
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Order... orders) {
        for (Order order : orders) {
            if (order != null) {
                this.orders.add(order);
            }
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl orderByIf(boolean condition, Order... orders) {
        return (AbstractMutableQueryImpl) MutableQuery.super.orderByIf(condition, orders);
    }

    @Override
    protected void onFrozen() {
        havingPredicates = mergePredicates(havingPredicates);
        super.onFrozen();
    }

    @Override
    public void disableSubQuery() {
        this.subQueryDisabledCount++;
    }

    @Override
    public void enableSubQuery() {
        this.subQueryDisabledCount--;
    }

    @Override
    public boolean isSubQueryDisabled() {
        return subQueryDisabledCount != 0;
    }

    void accept(
            AstVisitor visitor,
            List<Selection<?>> overriddenSelections,
            boolean withoutSortingAndPaging
    ) {
        Predicate predicate = getPredicate();
        Predicate havingPredicate = havingPredicates.isEmpty() ? null : havingPredicates.get(0);
        if (groupByExpressions.isEmpty() && !havingPredicates.isEmpty()) {
            throw new IllegalStateException(
                    "Having clause cannot be used without group clause"
            );
        }
        if (predicate != null) {
            ((Ast)predicate).accept(visitor);
        }
        for (Expression<?> expression : groupByExpressions) {
            ((Ast)expression).accept(visitor);
        }
        if (havingPredicate != null) {
            ((Ast)havingPredicate).accept(visitor);
        }
        AstContext astContext = visitor.getAstContext();
        if (withoutSortingAndPaging) {
            AstVisitor ignoredVisitor = new UseJoinOfIgnoredClauseVisitor(astContext);
            for (Order order : orders) {
                ((Ast)order.getExpression()).accept(ignoredVisitor);
            }
        } else {
            for (Order order : orders) {
                ((Ast)order.getExpression()).accept(visitor);
            }
        }
        if (overriddenSelections != null) {
            AstVisitor ignoredVisitor = new UseJoinOfIgnoredClauseVisitor(astContext);
            for (Selection<?> selection : overriddenSelections) {
                Ast.from(selection, astContext).accept(ignoredVisitor);
            }
        }
    }

    void renderTo(SqlBuilder builder, boolean withoutSortingAndPaging) {

        Predicate predicate = getPredicate();
        Predicate havingPredicate = havingPredicates.isEmpty() ? null : havingPredicates.get(0);

        TableImplementor<?> tableImplementor = getTableImplementor();
        tableImplementor.renderTo(builder);

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
                ((Ast)order.getExpression()).renderTo(builder);
                if (order.getOrderMode() == OrderMode.ASC) {
                    builder.sql(" asc");
                } else {
                    builder.sql(" desc");
                }
                switch (order.getNullOrderMode()) {
                    case NULLS_FIRST:
                        builder.sql(" nulls first");
                    case NULLS_LAST:
                        builder.sql(" nulls last");
                }
                separator = ", ";
            }
        }
    }

    protected boolean isGroupByClauseUsed() {
        return !this.groupByExpressions.isEmpty();
    }

    private static class UseJoinOfIgnoredClauseVisitor extends AstVisitor {

        public UseJoinOfIgnoredClauseVisitor(AstContext ctx) {
            super(ctx);
        }

        @Override
        public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
            return false;
        }

        @Override
        public void visitTableReference(TableImplementor<?> table, ImmutableProp prop) {
            handle(table, prop != null && prop.isId());
        }

        private void handle(TableImplementor<?> table, boolean isId) {
            if (table.getDestructive() != TableRowCountDestructive.NONE) {
                if (isId) {
                    getAstContext().useTableId(table);
                    use(table.getParent());
                } else {
                    use(table);
                }
            }
        }

        private void use(TableImplementor<?> table) {
            if (table != null) {
                getAstContext().useTable(table);
                use(table.getParent());
            }
        }
    }
}
