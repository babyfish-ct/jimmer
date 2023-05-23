package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SqlExpressions {

    private SqlExpressions() {}

    public static <T, E extends Expression<T>> E of(
            Class<T> type,
            String sql,
            Expression<?>[] expressions,
            Object[] values
    ) {
        return of(type, sql, it -> {
            if (expressions != null) {
                for (Expression<?> expression : expressions) {
                    it.expression(expression);
                }
            }
            if (values != null) {
                for (Object value : values) {
                    it.value(value);
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, E extends Expression<T>> E of(
            Class<T> type,
            String sql,
            Consumer<SqlExpressionContext> block
    ) {
        if (sql == null || sql.isEmpty()) {
            throw new IllegalArgumentException("sql cannot be null or empty");
        }
        List<Expression<?>> expressions = Collections.emptyList();
        List<Object> values = Collections.emptyList();
        if (block != null) {
            SqlExpressionContext ctx = new SqlExpressionContext();
            block.accept(ctx);
            expressions = ctx.getExpressions();
            values = ctx.getValues();
        }
        if (Boolean.class.isAssignableFrom(type)) {
            return (E)new Prd(sql, expressions, values);
        }
        if (String.class.isAssignableFrom(type)) {
            return (E)new Str(sql, expressions, values);
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return (E)new Num(type, sql, expressions, values);
        }
        if (Comparable.class.isAssignableFrom(type)) {
            return (E)new Cmp(type, sql, expressions, values);
        }
        return (E)new Any<T>(type, sql, expressions, values);
    }

    private static class Any<T> extends AbstractExpression<T> {

        private final Class<T> type;

        private final List<Object> parts;

        Any(Class<T> type, String sql, List<Expression<?>> expressions, List<Object> values) {
            List<Expression> literals;
            if (values.isEmpty()) {
                literals = Collections.emptyList();
            } else {
                literals = new ArrayList<>(values.size());
                for (Object value : values) {
                    if (value == null) {
                        throw new IllegalArgumentException("`values` cannot contain null");
                    }
                    literals.add(Expression.any().value(value));
                }
            }
            this.type = type;
            this.parts = parts(sql, expressions, literals);
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            for (Object part : parts) {
                if (part instanceof Ast) {
                    ((Ast) part).accept(visitor);
                }
            }
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            for (Object part : parts) {
                if (part instanceof Ast) {
                    renderChild((Ast) part, builder);
                } else {
                    builder.sql((String)part);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Any<?> any = (Any<?>) o;
            return type.equals(any.type) && parts.equals(any.parts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, parts);
        }
    }

    private static class Str extends Any<String> implements StringExpressionImplementor {

        Str(String sql, List<Expression<?>> expressions, List<Object> values) {
            super(String.class, sql, expressions, values);
        }
    }

    private static class Num<N extends Number & Comparable<N>> extends Any<N> implements NumericExpressionImplementor<N> {

        Num(Class<N> type, String sql, List<Expression<?>> expressions, List<Object> values) {
            super(type, sql, expressions, values);
        }
    }

    private static class Cmp<T extends Comparable<?>> extends Any<T> implements ComparableExpressionImplementor<T> {

        Cmp(Class<T> type, String sql, List<Expression<?>> expressions, List<Object> values) {
            super(type, sql, expressions, values);
        }
    }

    private static class Prd extends Any<Boolean> implements PredicateImplementor {

        Prd(String sql, List<Expression<?>> expressions, List<Object> values) {
            super(Boolean.class, sql, expressions, values);
        }
    }

    public static List<Object> parts(String sql, List<?> expressions, List<?> values) {
        List<Object> parts = new ArrayList<>();
        int size = sql.length();
        int start = 0;
        boolean isStr = false;
        int usedExpressionCount = 0;
        int usedValueCount = 0;
        for (int i = 0; i < size; i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                isStr = !isStr;
            } else if (!isStr && c == '%' && i + 1 < size) {
                char next = sql.charAt(i + 1);
                char nextNext = i + 2 < size ? sql.charAt(i + 2) : '\0';
                if (!Character.isLetter(nextNext) && !Character.isDigit(nextNext)) {
                    switch (next) {
                        case 'e':
                            if (start < i) {
                                parts.add(sql.substring(start, i));
                            }
                            start = i + 2;
                            if (usedExpressionCount >= expressions.size()) {
                                throw new IllegalArgumentException("Not enough expressions");
                            }
                            parts.add(expressions.get(usedExpressionCount++));
                            break;
                        case 'v':
                            if (start < i) {
                                parts.add(sql.substring(start, i));
                            }
                            start = i + 2;
                            if (usedValueCount >= values.size()) {
                                throw new IllegalArgumentException("Not enough values");
                            }
                            parts.add(values.get(usedValueCount++));
                            break;
                    }
                }
            }
        }
        if (usedExpressionCount < expressions.size()) {
            throw new IllegalArgumentException("Too many expressions");
        }
        if (usedValueCount < values.size()) {
            throw new IllegalArgumentException("Too many values");
        }
        if (start < size) {
            parts.add(sql.substring(start));
        }
        return parts;
    }
}
