package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.*;

public class NativeBuilderImpl<T> implements NativeBuilder<T> {

    protected final Class<T> type;

    private final String sql;

    private final List<Expression<?>> expressions = new ArrayList<>();

    private final List<Expression<?>> values = new ArrayList<>();

    NativeBuilderImpl(Class<T> type, String sql) {
        this.type = type;
        this.sql = sql;
    }

    @SuppressWarnings("unchecked")
    public static <T> NativeBuilder<T> any(Class<T> type, String sql) {
        if (String.class == type) {
            return (NativeBuilder<T>) new Str(sql);
        }
        if (boolean.class == type || Boolean.class == type) {
            return (NativeBuilder<T>) new Prd(sql);
        }
        if (Number.class.isAssignableFrom(type)) {
            return (NativeBuilder<T>) new Num<>((Class<Integer>) type, sql);
        }
        if (Comparable.class.isAssignableFrom(type)) {
            return (NativeBuilder<T>) new Cmp<>((Class<Comparable<?>>) type, sql);
        }
        return new NativeBuilderImpl<>(type, sql);
    }

    public static NativeBuilder.Str string(String sql) {
        return new Str(sql);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<?>> NativeBuilder.Cmp<T> comparable(Class<T> type, String sql) {
        if (Number.class.isAssignableFrom(type)) {
            return (NativeBuilder.Cmp<T>) new Num<>((Class<Integer>) type, sql);
        }
        return new Cmp<>(type, sql);
    }

    public static <N extends Number & Comparable<N>> NativeBuilder.Num<N> numeric(Class<N> type, String sql) {
        return new Num<>(type, sql);
    }

    public static NativeBuilder.Prd predicate(String sql) {
        return new Prd(sql);
    }

    @OldChain
    @Override
    @NotNull
    public NativeBuilder<T> expression(@NotNull Expression<?> expression) {
        Objects.requireNonNull(expression, "expression cannot be null");
        expressions.add(expression);
        return this;
    }

    @OldChain
    @Override
    @NotNull
    public NativeBuilder<T> value(@NotNull Object value) {
        Objects.requireNonNull(value, "value cannot be null");
        if (value instanceof Expression<?>) {
            throw new IllegalArgumentException(
                    "value() cannot accept expression, please call expression()"
            );
        }
        values.add(Literals.any(value));
        return this;
    }

    protected final List<Object> parts() {
        return parts(sql, expressions, values);
    }

    public static List<Object> parts(
            String sql,
            List<?> expressions,
            List<?> values
    ) {
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

    @Override
    public @NotNull Expression<T> build() {
        return new AnyExpression<>(type, parts());
    }

    private static class Str
            extends NativeBuilderImpl<String>
            implements NativeBuilder.Str {

        Str(String sql) {
            super(String.class, sql);
        }

        @NotNull
        @Override
        public NativeBuilder.Str expression(@NotNull Expression<?> expression) {
            return (NativeBuilder.Str)super.expression(expression);
        }

        @NotNull
        @Override
        public NativeBuilder.Str value(@NotNull Object value) {
            return (NativeBuilder.Str)super.value(value);
        }

        @Override
        public @NotNull StringExpression build() {
            return new StrExpression(parts());
        }
    }

    private static class Cmp<T extends Comparable<?>>
            extends NativeBuilderImpl<T>
            implements NativeBuilder.Cmp<T> {

        Cmp(Class<T> type, String sql) {
            super(type, sql);
        }

        @Override
        @NotNull
        public NativeBuilder.Cmp<T> expression(@NotNull Expression<?> expression) {
            return (NativeBuilder.Cmp<T>)super.expression(expression);
        }

        @Override
        @NotNull
        public NativeBuilder.Cmp<T> value(@NotNull Object value) {
            return (NativeBuilder.Cmp<T>)super.value(value);
        }

        @Override
        @NotNull
        public ComparableExpression<T> build() {
            return new CmpExpression<>(type, parts());
        }
    }

    private static class Num<N extends Number & Comparable<N>>
            extends Cmp<N>
            implements NativeBuilder.Num<N> {

        Num(Class<N> type, String sql) {
            super(type, sql);
        }

        @Override
        @NotNull
        public NativeBuilder.Num<N> expression(@NotNull Expression<?> expression) {
            return (NativeBuilder.Num<N>)super.expression(expression);
        }

        @Override
        @NotNull
        public NativeBuilder.Num<N> value(@NotNull Object value) {
            return (NativeBuilder.Num<N>)super.value(value);
        }

        @Override
        @NotNull
        public NumericExpression<N> build() {
            return new NumExpression<>(type, parts());
        }
    }

    private static class Dt<T extends Date & Comparable<Date>>
            extends Cmp<T>
            implements NativeBuilder.Dt<T> {

        Dt(Class<T> type, String sql) {
            super(type, sql);
        }

        @Override
        @NotNull
        public NativeBuilder.Dt<T> expression(@NotNull Expression<?> expression) {
            return (NativeBuilder.Dt<T>)super.expression(expression);
        }

        @Override
        @NotNull
        public NativeBuilder.Dt<T> value(@NotNull Object value) {
            return (NativeBuilder.Dt<T>)super.value(value);
        }

        @Override
        @NotNull
        public DateExpression<T> build() {
            return new DateExpression<>(type, parts());
        }
    }

    private static class Prd
            extends NativeBuilderImpl<Boolean>
            implements NativeBuilder.Prd {

        Prd(String sql) {
            super(Boolean.class, sql);
        }

        @NotNull
        @Override
        public NativeBuilder.Prd expression(@NotNull Expression<?> expression) {
            return (NativeBuilder.Prd)super.expression(expression);
        }

        @NotNull
        @Override
        public NativeBuilder.Prd value(@NotNull Object value) {
            return (NativeBuilder.Prd)super.value(value);
        }

        @Override
        public @NotNull Predicate build() {
            return new PrdExpression(parts());
        }
    }

    private static class AnyExpression<T> extends AbstractExpression<T> {

        private final Class<T> type;

        private final List<Object> parts;

        private AnyExpression(Class<T> type, List<Object> parts) {
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
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            for (Object part : parts) {
                if (part instanceof Ast) {
                    renderChild((Ast) part, builder);
                } else {
                    builder.sql((String)part);
                }
            }
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            return hasVirtualPredicate(parts);
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            ListIterator<Object> itr = this.parts.listIterator();
            while (itr.hasNext()) {
                Object part = itr.next();
                Object newPart = ctx.resolveVirtualPredicate(part);
                if (part == newPart) {
                    continue;
                }
                if (newPart == null) {
                    throw new IllegalArgumentException("Native SQL Expression does not support virtual predicate");
                }
                itr.set(newPart);
            }
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyExpression<?> any = (AnyExpression<?>) o;
            return type.equals(any.type) && parts.equals(any.parts);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, parts);
        }
    }

    private static class StrExpression extends AnyExpression<String> implements StringExpressionImplementor {

        StrExpression(List<Object> parts) {
            super(String.class, parts);
        }
    }

    private static class NumExpression<N extends Number & Comparable<N>> extends AnyExpression<N> implements NumericExpressionImplementor<N> {

        private NumExpression(Class<N> type, List<Object> parts) {
            super(type, parts);
        }
    }

    private static class DateExpression<T extends Date & Comparable<Date>> extends CmpExpression<T> implements DateExpressionImplementor<T> {

        private DateExpression(Class<T> type, List<Object> parts) {
            super(type, parts);
        }
    }

    private static class TemporalExpression<T extends Temporal & Comparable<?>> extends CmpExpression<T> implements TemporalExpressionImplementor<T> {

        private TemporalExpression(Class<T> type, List<Object> parts) {
            super(type, parts);
        }
    }

    private static class CmpExpression<T extends Comparable<?>> extends AnyExpression<T> implements ComparableExpressionImplementor<T> {

        private CmpExpression(Class<T> type, List<Object> parts) {
            super(type, parts);
        }
    }

    private static class PrdExpression extends AnyExpression<Boolean> implements PredicateImplementor {

        private PrdExpression(List<Object> parts) {
            super(Boolean.class, parts);
        }
    }
}
