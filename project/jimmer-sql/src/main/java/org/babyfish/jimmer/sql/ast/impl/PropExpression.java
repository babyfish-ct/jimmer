package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class PropExpression<T> extends AbstractExpression<T> {

    private TableImplementor<?> table;

    private ImmutableProp prop;

    public static PropExpression<?> of(TableImplementor<?> table, ImmutableProp prop) {
        Class<?> elementClass = prop.getElementClass();
        if (String.class.isAssignableFrom(elementClass)) {
            return new Str(table, prop);
        }
        if (elementClass.isPrimitive() || Number.class.isAssignableFrom(elementClass)) {
            return new Num(table, prop);
        }
        if (Comparable.class.isAssignableFrom(elementClass)) {
            return new Cmp<>(table, prop);
        }
        return new PropExpression<>(table, prop);
    }

    PropExpression(TableImplementor<?> table, ImmutableProp prop) {
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
    public int precedence() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getType() {
        return (Class<T>) prop.getElementClass();
    }

    private static class Str extends PropExpression<String> implements StringExpressionImplementor {

        Str(TableImplementor table, ImmutableProp prop) {
            super(table, prop);
        }
    }

    private static class Num extends PropExpression<Number> implements NumberExpressionImplementor<Number> {

        Num(TableImplementor<?> table, ImmutableProp prop) {
            super(table, prop);
        }

    }

    private static class Cmp<T extends Comparable<T>> extends PropExpression<T> implements ComparableExpressionImplementor<T> {

        Cmp(TableImplementor<?> table, ImmutableProp prop) {
            super(table, prop);
        }
    }
}
