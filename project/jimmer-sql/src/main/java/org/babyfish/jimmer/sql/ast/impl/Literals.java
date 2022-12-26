package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class Literals {

    public static StringExpression string(String value) {
        return new Str(value);
    }

    public static <N extends Number & Comparable<N>> NumericExpression<N> number(N value) {
        return new Num<>(value);
    }

    public static <T extends Comparable<?>> ComparableExpression<T> comparable(T value) {
        return new Cmp<>(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Expression<T> any(T value) {
        if (value instanceof String) {
            return (Expression<T>) string((String)value);
        }
        if (value instanceof Number) {
            return (Expression<T>) number((Number & Comparable)value);
        }
        if (value instanceof Comparable<?>) {
            return (Expression<T>) comparable((Comparable)value);
        }
        return new Any<>(value);
    }

    private static class Any<T> extends AbstractExpression<T> {

        private T value;

        public Any(T value) {
            if (value == null) {
                throw new IllegalArgumentException("The value of literal expression cannot be null");
            }
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<T> getType() {
            return (Class<T>)value.getClass();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.variable(value);
        }

        @Override
        public int precedence() {
            return 0;
        }
    }

    private static class Str extends Any<String> implements StringExpressionImplementor {
        public Str(String value) {
            super(value);
        }
    }

    private static class Num<N extends Number & Comparable<N>> extends Any<N> implements NumericExpressionImplementor<N> {
        public Num(N value) {
            super(value);
        }
    }

    private static class Cmp<T extends Comparable<?>> extends Any<T> implements ComparableExpressionImplementor<T> {
        public Cmp(T value) {
            super(value);
        }
    }
}
