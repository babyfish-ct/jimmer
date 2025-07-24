package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractMutableQueryImpl
        extends AbstractMutableStatementImpl
        implements MutableQuery {

    public static final int ORDER_BY_PRIORITY_STATEMENT = 0;

    public static final int ORDER_BY_PRIORITY_GLOBAL_FILTER = 1;

    public static final int ORDER_BY_PRIORITY_PROP_FILTER = 2;

    private final List<Expression<?>> groupByExpressions = new ArrayList<>();

    private List<Predicate> havingPredicates = new ArrayList<>();

    private final List<Order> orders = new ArrayList<>();

    private int orderByPriority = ORDER_BY_PRIORITY_STATEMENT;

    private int acceptedByPriority = ORDER_BY_PRIORITY_STATEMENT;

    protected AbstractMutableQueryImpl(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType
    ) {
        super(sqlClient, immutableType);
    }

    protected AbstractMutableQueryImpl(
            JSqlClientImplementor sqlClient,
            TableProxy<?> table
    ) {
        super(sqlClient, table);
    }

    protected AbstractMutableQueryImpl(
            JSqlClientImplementor sqlClient,
            BaseTable table
    ) {
        super(sqlClient, table);
    }

    @Override
    public AbstractMutableQueryImpl where(Predicate... predicates) {
        return (AbstractMutableQueryImpl) super.where(predicates);
    }

    /**
     * This method is deprecated, using {@code Dynamic Predicates}
     * is a more convenient approach.
     *
     * <p>Please look at this example:</p>
     * <pre>{@code
     * whereIf(name != null, table.name().eq(name))
     * }</pre>
     * When {@code name} is null, this code works because
     * {@code eq(null)} is automatically translated by Jimmer
     * to {@code isNull()}, which doesn't cause an exception.
     *
     * <p>Let's look at another example:</p>
     * <pre>{@code
     * whereIf(minPrice != null, table.price().ge(minPrice))
     * }</pre>
     * Except RUST marco, almost all programming languages
     * calculate all parameters first and then call the function.
     * Therefore, before {@code whereIf} is executed, {@code ge(null)}
     * already causes an exception.
     *
     * <p>For this reason, {@code whereIf} provides an overloaded form with a lambda parameter:</p>
     * <pre>{@code
     * whereIf(minPrice != null, () -> table.price().ge(minPrice))
     * }</pre>
     * Although this overloaded form can solve this problem,
     * it ultimately adds a mental burden during development.
     *
     * <p>Therefore, Jimmer provides {@code Dynamic Predicates}</p>
     *
     * <ul>
     *     <li>
     *         <b>Java</b>:
     *         eqIf, neIf, ltIf, leIf, gtIf, geIf, likeIf, ilikeIf, betweenIf
     *     </li>
     *     <li>
     *         <b>Kotlin</b>:
     *         eq?, ne?, lt?, le?, gt?, ge?, like?, ilike?, betweenIf?
     *     </li>
     * </ul>
     *
     * Taking Java's {@code geIf} as an example, this functionality
     * is ultimately implemented like this.
     * <pre>{@code
     * where(table.price().geIf(minPrice))
     * }</pre>
     */
    @OldChain
    @Override
    @Deprecated
    public AbstractMutableQueryImpl whereIf(boolean condition, Predicate predicate) {
        if (condition) {
            where(predicate);
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
        Order[] orders = new Order[expressions.length];
        for (int i = orders.length - 1; i >= 0; --i) {
            Expression<?> expression = expressions[i];
            if (expression != null) {
                orders[i] = new Order(expression, OrderMode.ASC, NullOrderMode.UNSPECIFIED);
            }
        }
        return orderBy(orders);
    }

    @Override
    public AbstractMutableQueryImpl orderByIf(boolean condition, Expression<?>... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl orderBy(Order... orders) {
        validateMutable();
        for (Order order : orders) {
            addOrder(order);
        }
        return this;
    }

    @Override
    public AbstractMutableQueryImpl orderByIf(boolean condition, Order... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @OldChain
    @Override
    public AbstractMutableQueryImpl orderBy(List<Order> orders) {
        validateMutable();
        for (Order order : orders) {
            addOrder(order);
        }
        return this;
    }

    @OldChain
    @Override
    public AbstractMutableQueryImpl orderByIf(boolean condition, List<Order> orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    public Predicate getHavingPredicate(AstContext astContext) {
        freeze(astContext);
        List<Predicate> ps = havingPredicates;
        return ps.isEmpty() ? null : ps.get(0);
    }

    @Override
    protected void onFrozen(AstContext astContext) {
        havingPredicates = mergePredicates(havingPredicates);
        super.onFrozen(astContext);
    }

    void accept(
            AstVisitor visitor,
            List<Selection<?>> overriddenSelections,
            boolean withoutSortingAndPaging
    ) {
        visitor.visitStatement(this);

        TableLikeImplementor<?> tableLikeImplementor = getTableLikeImplementor();
        tableLikeImplementor.accept(visitor);

        List<Predicate> havingPredicates = this.havingPredicates;
        if (groupByExpressions.isEmpty() && !havingPredicates.isEmpty()) {
            throw new IllegalStateException(
                    "Having clause cannot be used without group clause"
            );
        }

        Predicate havingPredicate = getHavingPredicate(visitor.getAstContext());
        for (Predicate predicate : unfrozenPredicates()) {
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

    void renderTo(SqlBuilder builder, boolean withoutSortingAndPaging, boolean reverseOrder) {
        TableLikeImplementor<?> tableLikeImplementor = getTableLikeImplementor();
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            SqlBuilder tmpBuilder = builder.createTempBuilder();
            renderClausesAfterTable(tmpBuilder, withoutSortingAndPaging, reverseOrder);
            tableLikeImplementor.renderTo(builder);
            builder.appendTempBuilder(tmpBuilder);
        } else {
            tableLikeImplementor.renderTo(builder);
            renderClausesAfterTable(builder, withoutSortingAndPaging, reverseOrder);
        }
    }

    private void renderClausesAfterTable(SqlBuilder builder, boolean withoutSortingAndPaging, boolean reverseOrder) {
        Predicate predicate = getPredicate(builder.getAstContext());
        Predicate havingPredicate = getHavingPredicate(builder.getAstContext());
        if (predicate != null) {
            builder.enter(SqlBuilder.ScopeType.WHERE);
            ((Ast) predicate).renderTo(builder);
            builder.leave();
        }
        if (!groupByExpressions.isEmpty()) {
            builder.enter(SqlBuilder.ScopeType.GROUP_BY);
            for (Expression<?> expression : groupByExpressions) {
                builder.separator();
                ((Ast)expression).renderTo(builder);
            }
            builder.leave();
        }
        if (havingPredicate != null) {
            builder.enter(SqlBuilder.ScopeType.HAVING);
            ((Ast) havingPredicate).renderTo(builder);
            builder.leave();
        }
        if (!withoutSortingAndPaging && !orders.isEmpty()) {
            if (reverseOrder) {
                builder.sql(" /* reverse sorting optimization */");
            }
            builder.enter(SqlBuilder.ScopeType.ORDER_BY);
            for (Order order : orders) {
                builder.separator();
                ((Ast)order.getExpression()).renderTo(builder);
                if (order.getOrderMode() == OrderMode.DESC) {
                    builder.sql(reverseOrder ? " asc" : " desc");
                } else {
                    builder.sql(reverseOrder ? " desc" : " asc");
                }
                switch (order.getNullOrderMode()) {
                    case NULLS_FIRST:
                        builder.sql(reverseOrder ? " nulls last" : " nulls first");
                        break;
                    case NULLS_LAST:
                        builder.sql(reverseOrder ? " nulls first" : " nulls last");
                        break;
                }
            }
            builder.leave();
        }
    }

    protected boolean isGroupByClauseUsed() {
        return !this.groupByExpressions.isEmpty();
    }

    @Override
    protected List<Expression<?>> getGroupExpressions() {
        return Collections.unmodifiableList(groupByExpressions);
    }

    @Override
    public List<Predicate> getHavingPredicates() {
        return Collections.unmodifiableList(havingPredicates);
    }

    @Override
    public void setHavingPredicates(List<Predicate> havingPredicates) {
        this.havingPredicates = havingPredicates;
    }

    @Override
    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public int getAcceptedOrderByPriority() {
        return acceptedByPriority;
    }

    public void setOrderByPriority(int priority) {
        this.orderByPriority = priority;
    }

    private void addOrder(Order order) {
        int priorityDiff = orderByPriority - acceptedByPriority;
        if (order == null || priorityDiff < 0) {
            return;
        }
        if (priorityDiff > 0) {
            this.orders.clear();
            acceptedByPriority = orderByPriority;
        }
        this.orders.add(order);
        modify();
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
        public void visitTableReference(RealTable table, ImmutableProp prop, boolean rawId) {
            TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
            if (implementor instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                handle(
                        table,
                        prop != null && prop.isId() &&
                                (rawId || TableUtils.isRawIdAllowed(tableImplementor, getAstContext().getSqlClient()))
                );
            }
        }

        private void handle(RealTable table, boolean isRawId) {
            TableLikeImplementor<?> implementor = table.getTableLikeImplementor();
            if (implementor instanceof TableImplementor<?>) {
                TableImplementor<?> tableImplementor = (TableImplementor<?>) implementor;
                if (tableImplementor.getDestructive() != TableRowCountDestructive.NONE) {
                    if (isRawId) {
                        getAstContext().useTableId(table);
                        use(table.getParent());
                    } else {
                        use(table);
                    }
                }
            }
        }

        private void use(RealTable table) {
            if (table != null) {
                getAstContext().useTable(table);
                use(table.getParent());
            }
        }
    }
}
