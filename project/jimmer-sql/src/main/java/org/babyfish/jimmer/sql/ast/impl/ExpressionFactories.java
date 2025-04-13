package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.*;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class ExpressionFactories {

    private ExpressionFactories() {}

    @SuppressWarnings("unchecked")
    public static <F> F of(Class<F> factoryType) {
        if (factoryType == Expression.StringFactory.class) {
            return (F)Str.INSTANCE;
        }
        if (factoryType == Expression.NumericFactory.class) {
            return (F)Num.INSTANCE;
        }
        if (factoryType == Expression.ComparableFactory.class) {
            return (F)Cmp.INSTANCE;
        }
        if (factoryType == Expression.AnyFactory.class) {
            return (F)Any.INSTANCE;
        }
        throw new IllegalStateException(
                "Unknown factory type \"" +
                        factoryType.getName() +
                        "\""
        );
    }

    private static class Str implements Expression.StringFactory {

        static final Str INSTANCE = new Str();

        @Override
        public @NotNull NativeBuilder.Str sqlBuilder(String sql) {
            return NativeBuilderImpl.string(sql);
        }

        @Override
        public <C> SimpleCaseBuilder.@NotNull Str<C> caseBuilder(C value) {
            return caseBuilder(Literals.any(value));
        }

        @Override
        public <C> SimpleCaseBuilder.@NotNull Str<C> caseBuilder(Expression<C> expression) {
            return new SimpleCaseBuilder.Str<>(expression);
        }

        @Override
        public CaseBuilder.@NotNull Str caseBuilder() {
            return new CaseBuilder.Str();
        }
    }

    private static class Num implements Expression.NumericFactory {

        static final Num INSTANCE = new Num();

        @Override
        public <N extends Number & Comparable<N>> NativeBuilder.@NotNull Num<N> sqlBuilder(Class<N> type, String sql) {
            return NativeBuilderImpl.numeric(type, sql);
        }

        @Override
        public <C, N extends Number & Comparable<N>> SimpleCaseBuilder.@NotNull Num<C, N> caseBuilder(Class<N> type, C value) {
            return caseBuilder(type, Literals.any(value));
        }

        @Override
        public <C, N extends Number & Comparable<N>> SimpleCaseBuilder.@NotNull Num<C, N> caseBuilder(Class<N> type, Expression<C> expression) {
            return new SimpleCaseBuilder.Num<>(type, expression);
        }

        @Override
        public <N extends Number & Comparable<N>> CaseBuilder.@NotNull Num<N> caseBuilder(Class<N> type) {
            return new CaseBuilder.Num<>(type);
        }
    }

    private static class Dt implements Expression.DateFactory {

        @Override
        public <T extends Date & Comparable<Date>> NativeBuilder.@NotNull Dt<T> sqlBuilder(Class<T> type, String sql) {
            return null;
        }

        @Override
        public <C, T extends Date & Comparable<Date>> SimpleCaseBuilder.@NotNull Cmp<C, T> caseBuilder(Class<T> type, C value) {
            return null;
        }

        @Override
        public <C, T extends Date & Comparable<Date>> SimpleCaseBuilder.@NotNull Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression) {
            return null;
        }

        @Override
        public <T extends Date & Comparable<Date>> CaseBuilder.@NotNull Cmp<T> caseBuilder(Class<T> type) {
            return null;
        }
    }

    private static class Cmp implements Expression.ComparableFactory {

        static final Cmp INSTANCE = new Cmp();

        @Override
        public <T extends Comparable<?>> NativeBuilder.@NotNull Cmp<T> sqlBuilder(Class<T> type, String sql) {
            return NativeBuilderImpl.comparable(type, sql);
        }

        @Override
        public <C, T extends Comparable<?>> SimpleCaseBuilder.@NotNull Cmp<C, T> caseBuilder(Class<T> type, C value) {
            return caseBuilder(type, Literals.any(value));
        }

        @Override
        public <C, T extends Comparable<?>> SimpleCaseBuilder.@NotNull Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression) {
            return new SimpleCaseBuilder.Cmp<>(type, expression);
        }

        @Override
        public <T extends Comparable<?>> CaseBuilder.@NotNull Cmp<T> caseBuilder(Class<T> type) {
            return new CaseBuilder.Cmp<>(type);
        }
    }

    private static class Any implements Expression.AnyFactory {

        static final Any INSTANCE = new Any();

        @Override
        public @NotNull <T> NativeBuilder<T> sqlBuilder(Class<T> type, String sql) {
            return NativeBuilderImpl.any(type, sql);
        }

        @Override
        public <C, T> @NotNull SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, C value) {
            return caseBuilder(type, Literals.any(value));
        }

        @Override
        public <C, T> @NotNull SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, Expression<C> expression) {
            return new SimpleCaseBuilder<>(type, expression);
        }

        @Override
        public <T> @NotNull CaseBuilder<T> caseBuilder(Class<T> type) {
            return new CaseBuilder<>(type);
        }
    }
}
