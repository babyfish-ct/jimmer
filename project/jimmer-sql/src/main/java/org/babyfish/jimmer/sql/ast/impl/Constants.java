package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Constants {

    public static <N extends Number & Comparable<N>> NumericExpression<N> number(N value) {
        return new Num<>(value);
    }

    public static StringExpression string(String value) {
        return new Str(value);
    }

    private static class Num<N extends Number & Comparable<N>> extends AbstractExpression<N> implements NumericExpressionImplementor<N> {

        private N value;

        public Num(N value) {
            this.value = Objects.requireNonNull(value, "`value` cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<N> getType() {
            return (Class<N>)value.getClass();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql(value.toString());
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            return false;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            return this;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Num<?> num = (Num<?>) o;
            return value.equals(num.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    private static class Str extends AbstractExpression<String> implements StringExpressionImplementor {

        private final String value;

        private Str(String value) {
            this.value = '\'' + value.replace("'", "''") + '\'';
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {}

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.variable(value);
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            return false;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            return this;
        }

        @Override
        public int precedence() {
            return 0;
        }
    }
}
