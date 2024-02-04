package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.lang.reflect.Array;
import java.util.*;

public class ComparisonPredicates {

    public static void renderComparison(
            ExpressionImplementor<?> left,
            String op,
            Object right,
            SqlBuilder builder
    ) {
        if (right instanceof LiteralExpressionImplementor<?>) {
            right = ((LiteralExpressionImplementor<?>)right).getValue();
        }
        boolean hasTuple = left instanceof TupleExpressionImplementor<?> &&
                (right instanceof TupleExpressionImplementor<?> || right instanceof TupleImplementor);
        boolean hasEmbedded = hasEmbedded(left);
        if (!hasTuple && !hasEmbedded) {
            render(left, builder);
            if (right == null && "=".equals(op)) {
                builder.sql(" is null");
            } else if (right == null && "<>".equals(op)) {
                builder.sql(" is not null");
            } else {
                builder.sql(" ").sql(op).sql(" ");
                render(right, left.getType(), left, builder);
            }
            return;
        }
        if (!"=".equals(op) && !"<>".equals(op)) {
            throw new ExecutionException("The \"" + op + "\" expression does not support tuple or embeddable");
        }
        List<Item> items = new ArrayList<>();
        ItemContext ctx = new ItemContext(builder.getAstContext().getSqlClient(), left, items);
        ctx.visit(right);
        if (items.isEmpty()) {
            throw new ExecutionException("The embedded value has no loaded properties");
        }
        if (builder.getAstContext().getSqlClient().getDialect().isTupleSupported() && !ctx.hasNull()) {
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            for (Item item : items) {
                builder.separator();
                render(item.left, builder);
            }
            builder.leave();
            builder.sql(" ").sql(op).sql(" ");
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            for (Item item : items) {
                builder.separator();
                render(item.right, item.left.getType(), item.left, builder);
            }
            builder.leave();
        } else {
            builder.enter(SqlBuilder.ScopeType.AND);
            for (Item item : items) {
                builder.separator();
                render(item.left, builder);
                if (item.right == null) {
                    builder.sql("=".equals(op) ? " is null" : "is not null");
                } else {
                    builder.sql(" ").sql(op).sql(" ");
                    render(item.right, item.left.getType(), item.left, builder);
                }
            }
            builder.leave();
        }
    }

    public static void renderInCollection(
            boolean negative,
            ExpressionImplementor<?> expr,
            Collection<?> values,
            SqlBuilder builder
    ) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        if (values.size() == 1) {
            renderComparison(expr, negative ? "<>" : "=", values.iterator().next(), builder);
            return;
        }
        boolean hasTuple = expr instanceof TupleExpressionImplementor<?>;
        boolean hasEmbedded = hasEmbedded(expr);
        if (!hasTuple && !hasEmbedded) {
            render(expr, builder);
            builder.sql(negative ? " not in " : " in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                builder.separator();
                render(value, expr.getType(), expr, builder);
            }
            builder.leave();
            return;
        }
        List<Item> prevItems = null;
        if (builder.getAstContext().getSqlClient().getDialect().isTupleSupported()) {
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            List<Item> items = new ArrayList<>();
            new ItemContext(builder.getAstContext().getSqlClient(), expr, items).visit(values.iterator().next());
            if (items.isEmpty()) {
                throw new ExecutionException("The embedded value has no loaded properties");
            }
            prevItems = items;
            for (Item item : items) {
                builder.separator();
                render(item.left, builder);
            }
            builder.leave();
            builder.sql(negative ? " not in " : " in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                builder.separator();
                if (items.isEmpty()) {
                    new ItemContext(builder.getAstContext().getSqlClient(), expr, items).visit(value);
                    if (!prevItems.equals(items)) {
                        throw new ExecutionException("The shape of values are not same, previous shape is " +
                                prevItems +
                                ", but the current shape is " +
                                items
                        );
                    }
                }
                builder.enter(SqlBuilder.ScopeType.TUPLE);
                for (Item item : items) {
                    builder.separator();
                    render(item.right, item.left.getType(), item.left, builder);
                }
                builder.leave();
                items.clear();
            }
            builder.leave();
        } else {
            boolean oneValue = values.size() == 1;
            if (!oneValue) {
                if (!negative) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            }
            for (Object value : values) {
                builder.separator();
                List<Item> items = new ArrayList<>();
                ItemContext ctx = new ItemContext(builder.getAstContext().getSqlClient(), expr, items);
                ctx.visit(value);
                if (prevItems == null) {
                    if (items.isEmpty()) {
                        throw new ExecutionException("The embedded value has no loaded properties");
                    }
                } else {
                    if (!prevItems.equals(items)) {
                        throw new ExecutionException("The shape of values are not same, previous shape is " +
                                prevItems +
                                ", but the current shape is " +
                                items
                        );
                    }
                }
                if (!oneValue) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.OR : SqlBuilder.ScopeType.AND);
                for (Item item : items) {
                    builder.separator();
                    render(item.left, builder);
                    builder.sql(negative ? " <> " : " = ");
                    render(item.right, item.left.getType(), item.left, builder);
                }
                builder.leave();
                if (!oneValue) {
                    builder.space('\n').sql(")");
                }
                prevItems = items;
            }
            if (!oneValue) {
                builder.leave();
                if (!negative) {
                    builder.space('\n').sql(")");
                }
            }
        }
    }

    private static void render(Expression<?> expr, SqlBuilder builder) {
        ((Ast)expr).renderTo(builder);
    }

    @SuppressWarnings("unchecked")
    private static void render(Object value, Class<?> type, Expression<?> matchedExpr, SqlBuilder builder) {
        if (value instanceof Expression<?>) {
            ((Ast)value).renderTo(builder);
        } else if (value != null) {
            ImmutableProp prop = null;
            if (matchedExpr instanceof PropExpressionImplementor<?>) {
                prop = ((PropExpressionImplementor<?>) matchedExpr).getProp();
            }
            builder.variable(Variables.process(value, prop, builder.getAstContext().getSqlClient()));
        } else {
            builder.nullVariable(type);
        }
    }

    private static boolean isEmbedded(ExpressionImplementor<?> expr) {
        PropExpressionImplementor<?> propExpr = (PropExpressionImplementor<?>) expr;
        return propExpr.getDeepestProp().isEmbedded(EmbeddedLevel.BOTH);
    }

    private static boolean hasEmbedded(ExpressionImplementor<?> expr) {
        if (expr instanceof TupleExpressionImplementor<?>) {
            TupleExpressionImplementor<?> tupleExpr = (TupleExpressionImplementor<?>) expr;
            for (int i = tupleExpr.size() - 1; i >= 0; --i) {
                Selection<?> selection = tupleExpr.get(i);
                if (!(selection instanceof ExpressionImplementor<?>)) {
                    throw new IllegalArgumentException("The sub item of tuple expression must be expression too");
                }
                if (hasEmbedded((ExpressionImplementor<?>) selection)) {
                    return true;
                }
            }
        } else {
            return isEmbedded(expr);
        }
        return false;
    }

    private static class Item {

        final List<Object> nodes;

        final ExpressionImplementor<?> left;

        final Object right;

        private Item(List<Object> nodes, ExpressionImplementor<?> left, Object right) {
            this.nodes = nodes;
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return nodes.equals(item.nodes);
        }

        @Override
        public int hashCode() {
            return nodes.hashCode();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Object o : nodes) {
                builder.append('/');
                if (o instanceof ImmutableProp) {
                    builder.append(((ImmutableProp)o).getName());
                } else {
                    builder.append(o);
                }
            }
            return builder.toString();
        }
    }

    private static class ItemContext {

        private static final Object UNLOADED = new Object();

        private final JSqlClientImplementor sqlClient;

        private final MetadataStrategy strategy;

        private final ExpressionImplementor<?> expr;

        private final List<Object> nodes;

        private final ItemContext root;

        private final List<Item> resultItems;

        private boolean hasNull;

        ItemContext(JSqlClientImplementor sqlClient, ExpressionImplementor<?> expr, List<Item> resultItems) {
            this.sqlClient = sqlClient;
            this.strategy = sqlClient.getMetadataStrategy();
            this.expr = expr;
            this.nodes = Collections.emptyList();
            this.root = this;
            this.resultItems = resultItems;
        }

        private ItemContext(ItemContext context, int index) {
            List<Object> nodes = new ArrayList<>(context.nodes.size() + 1);
            nodes.addAll(context.nodes);
            nodes.add(index);
            this.sqlClient = context.sqlClient;
            this.strategy = context.strategy;
            this.expr = item(context.expr, index);
            this.nodes = Collections.singletonList(nodes);
            this.root = context.root;
            this.resultItems = context.resultItems;
        }

        private ItemContext(ItemContext context, List<ImmutableProp> props) {
            List<Object> nodes = new ArrayList<>(context.nodes.size() + 1);
            nodes.addAll(context.nodes);
            nodes.addAll(props);
            this.sqlClient = context.sqlClient;
            this.strategy = context.strategy;
            this.expr = item(context.expr, props);
            this.nodes = nodes;
            this.root = context.root;
            this.resultItems = context.resultItems;
        }

        public boolean hasNull() {
            return hasNull;
        }

        @SuppressWarnings("unchecked")
        public void visit(Object right) {
            if (expr instanceof TupleExpressionImplementor<?>) {
                TupleExpressionImplementor<?> tupleExpr = (TupleExpressionImplementor<?>) expr;
                int size = tupleExpr.size();
                if (right instanceof TupleExpressionImplementor<?>) {
                    for (int i = 0; i < size; i++) {
                        new ItemContext(this, i).visit(item(right, i));
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        new ItemContext(this, i).visit(((TupleImplementor) right).get(i));
                    }
                }
            } else if (expr instanceof PropExpression.Embedded<?>) {
                Map<String, List<ImmutableProp>> pathMap =
                        ((PropExpressionImplementor<?>) expr).getDeepestProp().getTargetType().getEmbeddedPaths();
                if (right instanceof PropExpression.Embedded<?>) {
                    for (List<ImmutableProp> props : pathMap.values()) {
                        new ItemContext(this, props).visit(item(expr, props));
                    }
                } else {
                    for (List<ImmutableProp> props : pathMap.values()) {
                        Object value = itemValue(right, props);
                        if (value == UNLOADED) {
                            continue;
                        }
                        new ItemContext(this, props).visit(value);
                    }
                }
            } else {
                if (right instanceof Expression<?>) {
                    resultItems.add(new Item(nodes, expr, (ExpressionImplementor<?>) right));
                } else {
                    resultItems.add(new Item(nodes, expr, right));
                    root.hasNull |= right == null;
                }
            }
        }

        private static ExpressionImplementor<?> item(Object value, int index) {
            Selection<?> selection = ((TupleExpressionImplementor<?>) value).get(index);
            if (!(selection instanceof ExpressionImplementor<?>)) {
                throw new IllegalArgumentException("The tuple used by predicate can only contain sub expressions");
            }
            return (ExpressionImplementor<?>) selection;
        }

        private static ExpressionImplementor<?> item(ExpressionImplementor<?> expr, List<ImmutableProp> props) {
            for (ImmutableProp prop : props) {
                expr = ((PropExpression.Embedded<?>) expr).get(prop);
            }
            return expr;
        }

        private static Object itemValue(Object value, List<ImmutableProp> props) {
            for (ImmutableProp prop : props) {
                if (value == null) {
                    return null;
                }
                PropId propId = prop.getId();
                ImmutableSpi spi = (ImmutableSpi) value;
                if (!spi.__isLoaded(propId)) {
                    return UNLOADED;
                }
                value = spi.__get(propId);
            }
            return value;
        }
    }
}
