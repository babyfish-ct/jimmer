package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class PropExpression<T> extends AbstractExpression<T> {

    private TableImpl<?> table;

    private ImmutableProp prop;

    static PropExpression<?> of(TableImpl<?> table, ImmutableProp prop) {
        if (String.class.isAssignableFrom(prop.getElementClass())) {
            return new Str(table, prop);
        }
        if (Number.class.isAssignableFrom(prop.getElementClass())) {
            return new Num(table, prop);
        }
        if (Comparable.class.isAssignableFrom(prop.getElementClass())) {
            return new Cmp<>(table, prop);
        }
        return new PropExpression<>(table, prop);
    }

    PropExpression(TableImpl<?> table, ImmutableProp prop) {
        if (prop.isAssociation()) {
            throw new IllegalArgumentException("prop '" + prop + "' cannot be association property");
        }
        if (!(prop.getStorage() instanceof Column)) {
            throw new IllegalArgumentException("prop is not selectable");
        }
        this.table = table;
        this.prop = prop;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visitTableReference(table, prop);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        table.renderSelection(prop, builder);
    }

    @Override
    protected int precedence() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        return (Class<T>) prop.getElementClass();
    }

    private static class Str extends PropExpression<String> implements StringExpressionImplementor {

        Str(TableImpl table, ImmutableProp prop) {
            super(table, prop);
        }
    }

    private static class Num extends PropExpression<Number> implements NumberExpressionImplementor<Number> {

        Num(TableImpl<?> table, ImmutableProp prop) {
            super(table, prop);
        }

    }

    private static class Cmp<T extends Comparable<T>> extends PropExpression<T> implements ComparableExpressionImplementor<T> {

        Cmp(TableImpl<?> table, ImmutableProp prop) {
            super(table, prop);
        }
    }
}
