package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.sql.ast.*;
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
        if (!prop.isAssociation()) {
            throw new IllegalArgumentException("prop cannot be association property");
        }
        if (!(prop.getStorage() instanceof Column)) {
            throw new IllegalArgumentException("prop is not selectable");
        }
        this.table = table;
        this.prop = prop;
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        table.renderSelection(prop, builder);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visitTableReference(table, prop);
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

    private static class Str extends PropExpression<String> implements StringExpression {

        Str(TableImpl table, ImmutableProp prop) {
            super(table, prop);
        }

        @Override
        public Predicate like(String pattern) {
            return null;
        }

        @Override
        public Predicate ilike(String pattern) {
            return null;
        }
    }

    private static class Num extends PropExpression<Number> implements NumericExpression<Number> {

        Num(TableImpl<?> table, ImmutableProp prop) {
            super(table, prop);
        }


        @Override
        public NumericExpression<Number> plus(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public NumericExpression<Number> plus(Number other) {
            return null;
        }

        @Override
        public NumericExpression<Number> minus(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public NumericExpression<Number> minus(Number other) {
            return null;
        }

        @Override
        public NumericExpression<Number> prod(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public NumericExpression<Number> prod(Number other) {
            return null;
        }

        @Override
        public NumericExpression<Number> div(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public NumericExpression<Number> div(Number other) {
            return null;
        }

        @Override
        public NumericExpression<Number> rem(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public NumericExpression<Number> rem(Number other) {
            return null;
        }

        @Override
        public Predicate lt(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public Predicate lt(Number other) {
            return null;
        }

        @Override
        public Predicate le(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public Predicate le(Number other) {
            return null;
        }

        @Override
        public Predicate gt(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public Predicate gt(Number other) {
            return null;
        }

        @Override
        public Predicate ge(NumericExpression<Number> other) {
            return null;
        }

        @Override
        public Predicate ge(Number other) {
            return null;
        }

        @Override
        public NumericExpression<Number> sum() {
            return null;
        }

        @Override
        public NumericExpression<Number> avg() {
            return null;
        }
    }

    private static class Cmp<T extends Comparable<T>> extends PropExpression<T> implements ComparableExpression<T> {

        Cmp(TableImpl<?> table, ImmutableProp prop) {
            super(table, prop);
        }

        @Override
        public Predicate lt(ComparableExpression<T> other) {
            return null;
        }

        @Override
        public Predicate lt(T other) {
            return null;
        }

        @Override
        public Predicate le(ComparableExpression<T> other) {
            return null;
        }

        @Override
        public Predicate le(T other) {
            return null;
        }

        @Override
        public Predicate gt(ComparableExpression<T> other) {
            return null;
        }

        @Override
        public Predicate gt(T other) {
            return null;
        }

        @Override
        public Predicate ge(ComparableExpression<T> other) {
            return null;
        }

        @Override
        public Predicate ge(T other) {
            return null;
        }
    }
}
