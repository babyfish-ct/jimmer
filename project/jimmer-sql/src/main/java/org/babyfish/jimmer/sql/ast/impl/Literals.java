package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class Literals {

    private Literals() {}

    public static StringExpression string(String value) {
        return new Str(value);
    }

    public static <N extends Number & Comparable<N>> NumericExpression<N> number(N value) {
        return new Num<>(value);
    }

    public static <T extends Comparable<?>> ComparableExpression<T> comparable(T value) {
        return new Cmp<>(value);
    }

    public static void bindPropAndLiteral(Expression<?> mayBeProp, Expression<?> mayBeLiteral) {
        if (mayBeProp instanceof PropExpression<?> && mayBeLiteral instanceof Any<?>) {
            ((Any<?>)mayBeLiteral).setMatchedProp(((PropExpressionImplementor<?>)mayBeProp).getProp());
        }
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

        private ImmutableProp matchedProp;

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
            if (value != null && matchedProp != null) {
                ScalarProvider<Object, Object> scalarProvider = builder.getAstContext().getSqlClient().getScalarProvider(matchedProp);
                if (scalarProvider != null) {
                    try {
                        builder.variable(scalarProvider.toSql(value));
                        return;
                    } catch (Exception ex) {
                        throw new ExecutionException(
                                "Cannot convert the value \"" +
                                        value +
                                        "\" of prop \"" +
                                        matchedProp +
                                        "\" by the scalar provider \"" +
                                        scalarProvider.getClass().getName() +
                                        "\"",
                                ex
                        );
                    }
                }
            }
            builder.variable(value);
        }

        @Override
        public int precedence() {
            return 0;
        }

        public void setMatchedProp(ImmutableProp matchedProp) {
            if (this.matchedProp != null && this.matchedProp != matchedProp) {
                throw new IllegalStateException(
                        "The matched prop of current literal expression has been configured, " +
                                "is the current literal expression is shared by difference parts of SQL DSL?"
                );
            }
            this.matchedProp = matchedProp;
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
