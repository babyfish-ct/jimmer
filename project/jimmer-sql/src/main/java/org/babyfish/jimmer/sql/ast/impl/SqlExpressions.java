package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            return (E)new Num((Class<Number>)type, sql, expressions, values);
        }
        if (Comparable.class.isAssignableFrom(type)) {
            return (E)new Cmp(type, sql, expressions, values);
        }
        return (E)new Any<T>(type, sql, expressions, values);
    }

    private static class Any<T> extends AbstractExpression<T> {

        private Class<T> type;

        private List<Object> parts;

        Any(Class<T> type, String sql, List<Expression<?>> expressions, List<Object> values) {

            if (sql.indexOf('\'') != -1) {
                throw new IllegalArgumentException("SQL template cannot contains \"'\"");
            }

            int sqlLen = sql.length();
            List<Object> parts = new ArrayList<>();
            int index = 0;
            int usedExpressionCount = 0;
            int usedValueCount = 0;

            while (true) {
                int newIndex = sql.indexOf('%', index);
                if (newIndex == -1) {
                    break;
                }
                if (newIndex > index) {
                    parts.add(sql.substring(index, newIndex));
                }
                char partType = newIndex + 1 < sqlLen ? sql.charAt(newIndex + 1) : ' ';
                switch (partType) {
                    case 'e':
                        if (usedExpressionCount >= expressions.size()) {
                            throw new IllegalArgumentException("No enough expressions");
                        }
                        parts.add(expressions.get(usedExpressionCount++));
                        break;
                    case 'v':
                        if (usedValueCount >= values.size()) {
                            throw new IllegalArgumentException("No enough values");
                        }
                        parts.add(Literals.any(values.get(usedValueCount++)));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Illegal SQL template '" +
                                        sql +
                                        "', position: " +
                                        newIndex +
                                        ", only '%e' and '%v' are supported"
                        );
                }
                index = newIndex + 2;
            }
            if (usedExpressionCount < expressions.size()) {
                throw new IllegalArgumentException("Too many expression");
            }
            if (usedValueCount < values.size()) {
                throw new IllegalArgumentException("Too many values");
            }
            if (index < sqlLen) {
                parts.add(sql.substring(index));
            }
            this.type = type;
            this.parts = parts;
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
}
