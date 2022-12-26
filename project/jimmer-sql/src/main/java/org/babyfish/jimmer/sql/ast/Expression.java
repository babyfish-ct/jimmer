package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.Collection;
import java.util.function.Consumer;

public interface Expression<T> extends Selection<T> {

    /**
     * Check if two expressions are equal.
     *
     * <ul>
     *     <li>If {@code this} is null literal, returns {@code other.isNull()}</li>
     *     <li>{@code other} is null literal, returns {@code this.isNull()}</li>
     * </ul>
     *
     * @param other Right operand of expression
     * @return A predicate
     */
    Predicate eq(Expression<T> other);

    /**
     * Check if two expressions are equal.
     *
     * <ul>
     *     <li>If {@code other} is null, returns {@code this.isNull()}</li>
     * </ul>
     * @param other Right operand of expression
     * @return A predicate
     */
    Predicate eq(T other);

    /**
     * Check if two expressions are not equal.
     *
     * <ul>
     *     <li>If {@code this} is null literal, returns {@code other.isNotNull()}</li>
     *     <li>{@code other} is null literal, returns {@code this.isNotNull()}</li>
     * </ul>
     *
     * @param other Right operand of expression
     * @return A predicate
     */
    Predicate ne(Expression<T> other);

    /**
     * Check if two expressions are not equal.
     *
     * <ul>
     *     <li>If {@code other} is null, returns {@code this.isNotNull()}</li>
     * </ul>
     * @param other Right operand of expression
     * @return A predicate
     */
    Predicate ne(T other);

    Predicate isNull();

    Predicate isNotNull();

    Predicate in(Collection<T> values);

    Predicate notIn(Collection<T> values);

    Predicate in(TypedSubQuery<T> subQuery);

    Predicate notIn(TypedSubQuery<T> subQuery);

    NumericExpression<Long> count();

    NumericExpression<Long> count(boolean distinct);

    Expression<T> coalesce(T defaultValue);

    Expression<T> coalesce(Expression<T> defaultExpr);

    CoalesceBuilder<T> coalesceBuilder();

    Order asc();

    Order desc();

    static <N extends Number & Comparable<N>> NumericExpression<N> constant(N value) {
        return Constants.number(value);
    }

    static StringFactory string() {
        return ExpressionFactories.of(StringFactory.class);
    }

    static NumericFactory numeric() {
        return ExpressionFactories.of(NumericFactory.class);
    }

    static ComparableFactory comparable() {
        return ExpressionFactories.of(ComparableFactory.class);
    }


    static AnyFactory any() {
        return ExpressionFactories.of(AnyFactory.class);
    }

    static <T> Expression<T> nullValue(Class<T> type) {
        return any().nullValue(type);
    }

    static <T1, T2> Expression<Tuple2<T1, T2>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2
    ) {
        return new Tuples.Expr2<>(expr1, expr2);
    }

    static <T1, T2, T3> Expression<Tuple3<T1, T2, T3>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3
    ) {
        return new Tuples.Expr3<>(expr1, expr2, expr3);
    }

    static <T1, T2, T3, T4> Expression<Tuple4<T1, T2, T3, T4>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4
    ) {
        return new Tuples.Expr4<>(expr1, expr2, expr3, expr4);
    }

    static <T1, T2, T3, T4, T5> Expression<Tuple5<T1, T2, T3, T4, T5>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5
    ) {
        return new Tuples.Expr5<>(expr1, expr2, expr3, expr4, expr5);
    }

    static <T1, T2, T3, T4, T5, T6> Expression<Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6
    ) {
        return new Tuples.Expr6<>(expr1, expr2, expr3, expr4, expr5, expr6);
    }

    static <T1, T2, T3, T4, T5, T6, T7> Expression<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6,
            Expression<T7> expr7
    ) {
        return new Tuples.Expr7<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8> Expression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5,
            Expression<T6> expr6,
            Expression<T7> expr7,
            Expression<T8> expr8
    ) {
        return new Tuples.Expr8<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7, expr8);
    }

    static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Expression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> tuple(
        Expression<T1> expr1,
        Expression<T2> expr2,
        Expression<T3> expr3,
        Expression<T4> expr4,
        Expression<T5> expr5,
        Expression<T6> expr6,
        Expression<T7> expr7,
        Expression<T8> expr8,
        Expression<T9> expr9
    ) {
        return new Tuples.Expr9<>(expr1, expr2, expr3, expr4, expr5, expr6, expr7, expr8, expr9);
    }

    interface StringFactory {

        StringExpression value(String value);

        StringExpression sql(String sql);

        StringExpression sql(String sql, Expression<?> expression, Object ... values);

        StringExpression sql(String sql, Expression<?>[] expressions, Object ... values);

        StringExpression sql(String sql, Consumer<SqlExpressionContext> block);

        <C> SimpleCaseBuilder.Str<C> caseBuilder(C value);

        <C> SimpleCaseBuilder.Str<C> caseBuilder(Expression<C> expression);

        CaseBuilder.Str caseBuilder();
    }

    interface NumericFactory {

        <N extends Number & Comparable<N>> NumericExpression<N> value(N value);

        <N extends Number & Comparable<N>> NumericExpression<N> sql(Class<N> type, String sql);

        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, C value);

        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, Expression<C> expression);

        <N extends Number & Comparable<N>> CaseBuilder.Num<N> caseBuilder(Class<N> type);
    }

    /*
     * This class uses `T extends Comparable<?>`, not `T extends Comparable<T>`,
     * because not all types strictly implement the interface `Comparable<T>`,
     * such as `java.time.LocalDateTime`
     */
    interface ComparableFactory {

        <T extends Comparable<?>> ComparableExpression<T> value(T value);

        <T extends Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql);

        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, C value);

        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        <T extends Comparable<?>> CaseBuilder.Cmp<T> caseBuilder(Class<T> type);
    }

    interface AnyFactory {

        <T> Expression<T> value(T value);

        <T> Expression<T> nullValue(Class<T> type);

        <T> Expression<T> sql(Class<T> type, String sql);

        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, C value);

        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        <T> CaseBuilder<T> caseBuilder(Class<T> type);
    }
}
