package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

class ComplexPredicateRender {

    private final SqlBuilder builder;

    private final ExpressionImplementor<?> left;

    private final boolean hasTuple;

    private final boolean hasEmbedded;

    private final String op;

    private ExpressionImplementor<?> right;

    private List<Object> rightValues;

    private List<Item> items;

    private ComplexPredicateRender(SqlBuilder builder, ExpressionImplementor<?> left, boolean hasTuple, boolean hasEmbedded, String op) {
        this.builder = builder;
        this.left = left;
        this.hasTuple = hasTuple;
        this.hasEmbedded = hasEmbedded;
        this.op = op;
    }

    public static ComplexPredicateRender of(SqlBuilder builder, ExpressionImplementor<?> left, String op) {
        boolean hasTuple = left instanceof TupleExpressionImplementor<?>;
        boolean hasEmbedded = hasEmbedded(left);
        if (!hasTuple && !hasEmbedded) {
            return null;
        }
        return new ComplexPredicateRender(builder, left, hasTuple, hasEmbedded, op);
    }

    public void add(Object right) {
        if (right instanceof ExpressionImplementor<?>) {
            ExpressionImplementor<?> expr = (ExpressionImplementor<?>) right;
            if (expr instanceof Literals.Any) {
                addValue(((Literals.Any<?>)expr).getValue());
            } else if (expr instanceof NullExpression<?>) {
                addValue(null);
            } else {
                addExpr((ExpressionImplementor<?>) right);
            }
        } else {
            addValue(right);
        }
    }

    public void render() {
        switch (items.size()) {
            case 0:
                builder.sql("in".equals(op) ? "1 = 0" : "1 = 1");
                break;
            case 1:
                ((Ast)left).renderTo(builder);
                builder.sql(" ").sql(op).sql(" ");
                if (right != null) {
                    ((Ast)right).renderTo(builder);
                } else {
                    for (Object value : rightValues) {

                    }
                }
                break;
        }
    }

    private void renderValue(int index, Object value) {
        Item item = items.get(index);
        if (item.props.isEmpty()) {

        }
    }

    private void addExpr(ExpressionImplementor<?> right) {
        if (this.right != null || this.rightValues != null) {
            throw new IllegalStateException("The right expression has been set");
        }
        this.right = right;
    }

    private void addValue(Object value) {
        if (this.right != null) {
            throw new IllegalStateException("The right expression has been set");
        }
        if (rightValues == null) {
            List<Item> items = new ArrayList<>();
            new ItemContext(builder.getAstContext().getSqlClient().getMetadataStrategy(),  left, items).visit(value);
            if (items.isEmpty()) {
                throw new IllegalArgumentException("No property of the embeddable object is specified");
            }
            List<Object> values = new ArrayList<>();
            values.add(value);
            this.items = items;
            this.rightValues = values;
        } else {
            validateSameShape(rightValues.get(0), value);
            this.rightValues.add(value);
        }
    }

    private static boolean isEmbedded(ExpressionImplementor<?> expr) {
        PropExpressionImpl<?> propExpr = (PropExpressionImpl<?>) expr;
        return propExpr.deepestProp.isEmbedded(EmbeddedLevel.BOTH);
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

    private void validateSameShape(Object a, Object b) {
        if (a == null || b == null) {
            return;
        }
        if (a instanceof TupleImplementor && b instanceof TupleImplementor) {
            TupleImplementor ta = (TupleImplementor) a;
            TupleImplementor tb = (TupleImplementor) b;
            if (ta.size() != tb.size()) {
                throw new IllegalArgumentException(
                        "The shape of values \"" +
                                a +
                                "\" and \"" +
                                b +
                                "\" are different"
                );
            }
            for (int i = ta.size() - 1; i >= 0; --i) {
                validateSameShape(ta.get(i), tb.get(i));
            }
        }
        ImmutableType type = ImmutableType.tryGet(a.getClass());
        if (type == null) {
            return;
        }
        ImmutableSpi ia = (ImmutableSpi) a;
        ImmutableSpi ib = (ImmutableSpi) b;
        for (ImmutableProp prop : type.getProps().values()) {
            PropId propId = prop.getId();
            if (ia.__isLoaded(propId) != ib.__isLoaded(propId)) {
                throw new IllegalArgumentException(
                        "The shape of values \"" +
                                a +
                                "\" and \"" +
                                b +
                                "\" are different"
                );
            }
            validateSameShape(ia.__get(propId), ib.__get(propId));
        }
    }

    private static class Item {

        private final String columnName;

        private final List<ImmutableProp> props;

        private Item(String columnName, List<ImmutableProp> props) {
            this.columnName = columnName;
            this.props = props;
        }

        public String getColumnName() {
            return columnName;
        }

        public Object get(Object embeddable) {
            Object value = embeddable;
            if (value != null) {
                for (ImmutableProp prop : props) {
                    value = ((ImmutableSpi)value).__get(prop.getId());
                    if (value == null) {
                        break;
                    }
                }
            }
            return value;
        }
    }

    private static class ItemContext {

        private final MetadataStrategy strategy;

        private final Selection<?> selection;

        private final List<Item> items;

        private ItemContext(MetadataStrategy strategy, Selection<?> selection, List<Item> items) {
            this.strategy = strategy;
            this.selection = selection;
            this.items = items;
        }

        public void visit(Object value) {
            if (selection instanceof TupleExpressionImplementor<?>) {
                TupleExpressionImplementor<?> tupleExpr = (TupleExpressionImplementor<?>) selection;
                TupleImplementor tuple = (TupleImplementor) value;
                if (tuple != null && tuple.size() != tuple.size()) {
                    throw new IllegalArgumentException("The tuple does not match the tuple expression");
                }
                int size = tupleExpr.size();
                for (int i = 0; i < size; i++) {
                    Selection<?> subSelect = tupleExpr.get(i);
                    if (!(subSelect instanceof ExpressionImplementor<?>)) {

                    }
                    new ItemContext(strategy, subSelect, items).visit(tuple != null ? tuple.get(i) : null);
                }
            } else if (selection instanceof PropExpressionImpl<?>){
                PropExpressionImpl<?> propExpr = (PropExpressionImpl<?>) selection;
                ImmutableProp prop = propExpr.getProp();
                Map<String, List<ImmutableProp>> pathMap = prop.getTargetType() != null ?
                        prop.getTargetType().getEmbeddedPaths() :
                        Collections.emptyMap();
                if (pathMap.isEmpty()) {
                    items.add(new Item(prop.<SingleColumn>getStorage(strategy).name(0), Collections.singletonList(prop)));
                } else {
                    for (Map.Entry<String, List<ImmutableProp>> e : pathMap.entrySet()) {
                        if (isLoaded(value, e.getValue())) {
                            String columnName = prop.<EmbeddedColumns>getStorage(strategy).partial(e.getKey()).name(0);
                            items.add(new Item(columnName, e.getValue()));
                        }
                    }
                }
            }
        }

        private static boolean isLoaded(Object value, List<ImmutableProp> props) {
            if (value == null) {
                return true;
            }
            for (ImmutableProp prop : props) {
                ImmutableSpi spi = (ImmutableSpi)value;
                if (!spi.__isLoaded(prop.getId())) {
                    return false;
                }
                value = spi.__get(prop.getId());
                if (value == null) {
                    break;
                }
            }
            return true;
        }
    }
}
