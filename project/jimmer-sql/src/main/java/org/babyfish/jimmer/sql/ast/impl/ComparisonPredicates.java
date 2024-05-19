package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
            renderExpr(left, builder);
            if (right == null && "=".equals(op)) {
                builder.sql(" is null");
            } else if (right == null && "<>".equals(op)) {
                builder.sql(" is not null");
            } else {
                builder.sql(" ").sql(op).sql(" ");
                renderValue(right, left.getType(), left, builder);
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
                renderExpr(item.left, builder);
            }
            builder.leave();
            builder.sql(" ").sql(op).sql(" ");
            builder.enter(SqlBuilder.ScopeType.TUPLE);
            for (Item item : items) {
                builder.separator();
                renderValue(item.right, item.left.getType(), item.left, builder);
            }
            builder.leave();
        } else {
            builder.enter(SqlBuilder.ScopeType.AND);
            for (Item item : items) {
                builder.separator();
                renderExpr(item.left, builder);
                if (item.right == null) {
                    builder.sql("=".equals(op) ? " is null" : "is not null");
                } else {
                    builder.sql(" ").sql(op).sql(" ");
                    renderValue(item.right, item.left.getType(), item.left, builder);
                }
            }
            builder.leave();
        }
    }

    public static void renderInCollection(
            boolean nullable,
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
            JSqlClientImplementor sqlClient = builder.getAstContext().getSqlClient();
            if (sqlClient.isInListToAnyEqualityEnabled()) {
                ImmutableProp prop = propOf(expr);
                if (prop != null) {
                    String sqlType = prop
                            .<SingleColumn>getStorage(sqlClient.getMetadataStrategy())
                            .getSqlType();
                    if (sqlType != null) {
                        renderEqArray(
                                nullable,
                                negative,
                                values,
                                builder,
                                new ExprRender(expr),
                                sqlType,
                                value -> Variables.process(value, prop, sqlClient)
                        );
                        return;
                    }
                }
            }
            renderInRawList(
                    nullable,
                    negative,
                    values,
                    builder,
                    new ExprRender(expr),
                    (b, value) -> renderValue(value, expr.getType(), expr, b)
            );
            return;
        }
        if (builder.getAstContext().getSqlClient().getDialect().isTupleSupported()) {
            List<Item> items = new ArrayList<>();
            new ItemContext(builder.getAstContext().getSqlClient(), expr, items).visit(values.iterator().next());
            if (items.isEmpty()) {
                throw new ExecutionException("The embedded value has no loaded properties");
            }
            List<Item> prevItems = new ArrayList<>(items);
            renderInRawList(
                    nullable,
                    negative,
                    values,
                    builder,
                    new ExprRender(expr) {
                        @Override
                        void render(SqlBuilder b) {
                            b.enter(SqlBuilder.ScopeType.TUPLE);
                            for (Item item : prevItems) {
                                b.separator();
                                renderExpr(item.left, b);
                            }
                            b.leave();
                        }
                    },
                    (b, value) -> {
                        if (items.isEmpty()) {
                            new ItemContext(b.getAstContext().getSqlClient(), expr, items).visit(value);
                            if (!prevItems.equals(items)) {
                                throw new ExecutionException("The shape of values are not same, previous shape is " +
                                        prevItems +
                                        ", but the current shape is " +
                                        items
                                );
                            }
                        }
                        try {
                            b.enter(SqlBuilder.ScopeType.TUPLE);
                            for (Item item : items) {
                                b.separator();
                                renderValue(item.right, item.left.getType(), item.left, b);
                            }
                            b.leave();
                        } finally {
                            items.clear();
                        }
                    }
            );
        } else {
            renderSimplePredicates(
                    negative,
                    expr,
                    values,
                    builder
            );
        }
    }

    private static void renderExpr(Expression<?> expr, SqlBuilder builder) {
        Ast.of(expr).renderTo(builder);
    }

    @SuppressWarnings("unchecked")
    private static void renderValue(Object value, Class<?> type, Expression<?> matchedExpr, SqlBuilder builder) {
        if (value instanceof Expression<?>) {
            Ast.of((Expression<?>) value).renderTo(builder);
        } else if (value != null) {
            ImmutableProp prop = propOf(matchedExpr);
            builder.variable(
                    prop != null ?
                            Variables.process(value, prop, builder.getAstContext().getSqlClient()) :
                            Variables.process(value, type, builder.getAstContext().getSqlClient())
            );
        } else {
            builder.nullVariable(type);
        }
    }

    private static ImmutableProp propOf(Expression<?> expr) {
        if (expr instanceof PropExpressionImplementor<?>) {
            return ((PropExpressionImplementor<?>) expr).getDeepestProp();
        }
        return null;
    }

    private static boolean isEmbedded(ExpressionImplementor<?> expr) {
        if (!(expr instanceof PropExpressionImplementor<?>)) {
            return false;
        }
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

        Item(List<Object> nodes, ExpressionImplementor<?> left, Object right) {
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

    private static void renderEqArray(
            boolean nullable,
            boolean negative,
            Collection<?> values,
            SqlBuilder builder,
            ExprRender exprRender,
            String sqlType,
            Function<Object, Object> valueConverter
    ) {
        if (nullable) {
            Collection<Object> nonNullValues = new ArrayList<>(values.size());
            boolean hasNullable = false;
            for (Object value : values) {
                if (value == null) {
                    hasNullable = true;
                } else {
                    nonNullValues.add(value);
                }
            }
            builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            renderEqArrayImpl(
                    negative,
                    nonNullValues,
                    builder,
                    exprRender,
                    sqlType,
                    valueConverter
            );
            if (hasNullable) {
                builder.separator();
                exprRender.render(builder);
                builder.sql(" is null");
            }
            builder.leave();
        } else {
            renderEqArrayImpl(
                    negative,
                    values,
                    builder,
                    exprRender,
                    sqlType,
                    valueConverter
            );
        }
    }

    private static void renderEqArrayImpl(
            boolean negative,
            Collection<?> values,
            SqlBuilder builder,
            ExprRender exprRender,
            String sqlType,
            Function<Object, Object> valueConverter
    ) {
        Object[] arr = new Object[values.size()];
        int index = 0;
        for (Object value : values) {
            arr[index++] = valueConverter.apply(value);
        }

        exprRender.render(builder);
        builder.sql(negative ? " <> any(" : " = any(");
        builder.variable(new TypedList<>(sqlType, arr));
        builder.sql(")");
    }

    private static void renderInRawList(
            boolean nullable,
            boolean negative,
            Collection<?> values,
            SqlBuilder builder,
            ExprRender exprReader,
            BiConsumer<SqlBuilder, Object> valueRender
    ) {
        if (nullable) {
            builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            InvalidValueRecorder recorder = new InvalidValueRecorder();
            SqlBuilder childBuilder = builder.createChildBuilder(true, true);
            renderInRawListImpl(
                    negative,
                    values,
                    childBuilder,
                    exprReader,
                    valueRender,
                    recorder
            );
            if (!recorder.hasValidValues) {
                childBuilder.abort();
            }
            childBuilder.build();
            if (!recorder.isEmpty()) {
                builder.separator();
                renderSimplePredicates(negative, exprReader.expr, recorder, builder);
            }
            builder.leave();
        } else {
            renderInRawListImpl(
                    negative,
                    values,
                    builder,
                    exprReader,
                    valueRender,
                    null
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderInRawListImpl(
            boolean negative,
            Collection<?> values,
            SqlBuilder builder,
            ExprRender exprReader,
            BiConsumer<SqlBuilder, Object> valueRender,
            InvalidValueRecorder recorder
    ) {
        JSqlClientImplementor sqlClient = builder.getAstContext().getSqlClient();
        if (values.size() > sqlClient.getDialect().getMaxInListSize()) {
            InList<Object> parts = new InList<>(
                    (Collection<Object>) values,
                    sqlClient.isInListPaddingEnabled(),
                    sqlClient.getDialect().getMaxInListSize()
            );
            InList.Committer committer = recorder != null ? parts.committer() : null;
            builder.sql("(").enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            for (Iterable<Object> part : parts) {
                builder.separator();
                exprReader.render(builder);
                builder.sql(negative ? " not in " : " in ");
                builder.enter(SqlBuilder.ScopeType.LIST);
                for (Object value : part) {
                    if (recorder != null) {
                        SqlBuilder childBuilder = builder.createChildBuilder(true, true);
                        try {
                            childBuilder.separator();
                            valueRender.accept(childBuilder, value);
                            recorder.addValid();
                            committer.commit();
                        } catch (SqlBuilder.NullVariableException ex) {
                            childBuilder.abort();
                            recorder.add(value);
                        }
                        childBuilder.build();
                    } else {
                        builder.separator();
                        valueRender.accept(builder,value);
                    }
                }
                builder.leave();
            }
            builder.leave().sql(")");
        } else {
            Iterable<?> iterable =
                    sqlClient.isInListPaddingEnabled() ?
                            new InList<>((Collection<Object>)values, true, Integer.MAX_VALUE).iterator().next() :
                            values;
            InList.Committer committer = iterable instanceof InList<?> ? ((InList<?>)iterable).committer() : null;
            exprReader.render(builder);
            builder.sql(negative ? " not in " : " in ");
            builder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : iterable) {
                if (recorder != null) {
                    SqlBuilder childBuilder = builder.createChildBuilder(true, true);
                    try {
                        childBuilder.separator();
                        valueRender.accept(childBuilder, value);
                        recorder.addValid();
                        if (committer != null) {
                            committer.commit();
                        }
                    } catch (SqlBuilder.NullVariableException ex) {
                        childBuilder.abort();
                        recorder.add(value);
                    }
                    childBuilder.build();
                } else {
                    builder.separator();
                    valueRender.accept(builder,value);
                }
            }
            builder.leave();
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderSimplePredicates(
            boolean negative,
            ExpressionImplementor<?> expr,
            Collection<?> values,
            SqlBuilder builder
    ) {
        List<Item> prevItems = null;
        boolean oneValue = values.size() == 1;
        if (!oneValue) {
            if (!negative) {
                builder.sql("(").space('\n');
            }
            builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
        }
        Iterable<?> iterable = builder.getAstContext().getSqlClient().isExpandedInListPaddingEnabled() ?
                new InList<Object>((Collection<Object>) values, true, Integer.MAX_VALUE)
                        .iterator().next():
                values;
        for (Object value : iterable) {
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
                renderExpr(item.left, builder);
                if (item.right == null) {
                    builder.sql(negative ? " is not null" : " is null");
                } else {
                    builder.sql(negative ? " <> " : " = ");
                    renderValue(item.right, item.left.getType(), item.left, builder);
                }
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
            this.nodes = Collections.unmodifiableList(nodes);
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
            } else if (expr instanceof PropExpressionImplementor<?> && ((PropExpressionImplementor<?>)expr).getDeepestProp().isEmbedded(EmbeddedLevel.BOTH)) {
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
                expr = ((PropExpression.Embedded<?>)((PropExpressionImplementor<?>) expr).unwrap()).get(prop);
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

    private static class ExprRender {

        private final ExpressionImplementor<?> expr;

        ExprRender(ExpressionImplementor<?> expr) {
            this.expr = expr;
        }

        void render(SqlBuilder builder) {
            Ast.of(expr).renderTo(builder);
        }
    }

    private static class InvalidValueRecorder extends LinkedHashSet<Object> {

        boolean hasValidValues;

        void addValid() {
            hasValidValues = true;
        }
    }
}
