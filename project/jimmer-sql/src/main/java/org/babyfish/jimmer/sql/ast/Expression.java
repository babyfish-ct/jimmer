package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.util.RowCounts;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public interface Expression<T> extends Selection<T> {

    /**
     * Create `equal` predicate or `is null` predicate.
     *
     * <ul>
     *     <li>If {@code this} is null literal, returns {@code other.isNull()}</li>
     *     <li>{@code other} is null literal, returns {@code this.isNull()}</li>
     * </ul>
     *
     * @param other The right operand which cannot be null
     * @return `equal` predicate or `is null` predicate
     * @exception NullPointerException The argument `other` is null
     */
    @NotNull
    Predicate eq(Expression<T> other);

    /**
     * Create `equal` predicate or `is null` predicate.
     *
     * <p>If {@code other} is null, creates an `is null` predicate</p>
     *
     * @param other The right operand which can be null
     * @return `equal` predicate or `is null` predicate
     */
    @NotNull
    Predicate eq(@Nullable T other);

    /**
     * Create `equal` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other}
     *                  is neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *              returns null directly; otherwise, check if {@code condition}
     *              is true to decide whether to create predicate
     * @return A predicate or null
     * @see #eqIf(Object)
     */
    @Nullable
    default Predicate eqIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? eq(other) : null;
    }

    /**
     * Create `equal` predicate when certain conditions are met.
     *
     * @param other The right operand which can be null, if it is null or empty string,
     *              returns null directly; otherwise, create predicate
     * @return A predicate or null
     * @see #eqIf(boolean, Object)
     */
    @Nullable
    default Predicate eqIf(@Nullable T other) {
        return eqIf(true, other);
    }

    /**
     * Create `not equal` predicate or `is not null` predicate.
     *
     * <ul>
     *     <li>If {@code this} is null literal, returns {@code other.isNotNull()}</li>
     *     <li>{@code other} is null literal, returns {@code this.isNotNull()}</li>
     * </ul>
     *
     * @param other The right operand which cannot be null
     * @return `not equal` predicate or `is not null` predicate
     * @exception NullPointerException The argument `other` is null
     */
    @NotNull
    Predicate ne(Expression<T> other);

    /**
     * Create `not equal` predicate or `is not null` predicate.
     *
     * <p>If {@code other} is null, creates an `is not null` predicate</p>
     *
     * @param other The right operand which can be null
     * @return `not equal` predicate or `is not null` predicate
     */
    @NotNull
    Predicate ne(@Nullable T other);

    /**
     * Create `not equal` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other}
     *                  is neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *              returns null directly; otherwise, check if {@code condition}
     *              is true to decide whether to create predicate
     * @return `not equal` predicate or null
     * @see #neIf(Object)
     */
    @Nullable
    default Predicate neIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? ne(other) : null;
    }

    /**
     * Create `not equal` predicate when certain conditions are met.
     *
     * @param other The right operand which can be null, if it is null or empty string,
     *              returns null directly; otherwise, create predicate
     * @return `not equal` predicate or null
     * @see #eqIf(boolean, Object)
     */
    @Nullable
    default Predicate neIf(@Nullable T other) {
        return neIf(true, other);
    }

    @NotNull
    Predicate isNull();

    @NotNull
    Predicate isNotNull();

    /**
     * Create `in` predicate
     * 
     * @param values A collection which cannot be null
     * @return `in` predicate
     * @exception NullPointerException The argument {@code values} is null
     */
    @NotNull
    Predicate in(Collection<T> values);

    /**
     * Create `in` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other} is not null, creates predicate;
     *                  otherwise, returns null
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate inIf(boolean condition, @Nullable Collection<T> values) {
        return condition && values != null ? in(values) : null;
    }

    /**
     * Create `in` predicate when certain conditions are met.
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate inIf(@Nullable Collection<T> values) {
        return inIf(true, values);
    }

    /**
     * Create `not in` predicate
     *
     * @param values A collection which cannot be null
     * @return `not in` predicate
     * @exception NullPointerException The argument {@code values} is null
     */
    @NotNull
    Predicate notIn(Collection<T> values);

    /**
     * Create `not in` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code values} is not null, creates predicate;
     *                  otherwise, returns null
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate notInIf(boolean condition, @Nullable Collection<T> values) {
        return condition && values != null ? notIn(values) : null;
    }

    /**
     * Create `not in` predicate when certain conditions are met.
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate notInIf(@Nullable Collection<T> values) {
        return notInIf(true, values);
    }

    /**
     * Create `in` predicate
     *
     * @param subQuery A sub query which cannot be null
     * @return `in` predicate
     * @exception NullPointerException The argument {@code subQuery} is null
     */
    @NotNull
    Predicate in(TypedSubQuery<T> subQuery);

    /**
     * Create `in` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code subQuery} is not null, creates predicate;
     *                  otherwise, returns null
     * @param subQuery The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate inIf(boolean condition, @Nullable TypedSubQuery<T> subQuery) {
        return condition && subQuery != null ? in(subQuery) : null;
    }

    /**
     * Create `in` predicate when certain conditions are met.
     * @param subQuery The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate inIf(@Nullable TypedSubQuery<T> subQuery) {
        return inIf(true, subQuery);
    }

    /**
     * Create `not in` predicate
     *
     * @param subQuery A sub query which cannot be null
     * @return `not in` predicate
     * @exception NullPointerException The argument {@code subQuery} is null
     */
    @NotNull
    Predicate notIn(TypedSubQuery<T> subQuery);

    /**
     * Create `not in` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code subQuery} is not null, creates predicate;
     *                  otherwise, returns null
     * @param subQuery The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate notInIf(boolean condition, @Nullable TypedSubQuery<T> subQuery) {
        return condition && subQuery != null ? notIn(subQuery) : null;
    }

    /**
     * Create `not in` predicate when certain conditions are met.
     * @param subQuery The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate notInIf(@Nullable TypedSubQuery<T> subQuery) {
        return notInIf(true, subQuery);
    }

    @NotNull
    NumericExpression<Long> count();

    @NotNull
    NumericExpression<Long> count(boolean distinct);

    @NotNull
    Expression<T> coalesce(T defaultValue);

    @NotNull
    Expression<T> coalesce(Expression<T> defaultExpr);

    @NotNull
    CoalesceBuilder<T> coalesceBuilder();

    @NotNull
    Order asc();

    @NotNull
    Order desc();

    @NotNull
    static <N extends Number & Comparable<N>> NumericExpression<N> constant(N value) {
        return Constants.number(value);
    }

    @NotNull
    static StringExpression constant(String value) {
        return Constants.string(value);
    }

    /**
     * Global expression for row count across tables
     *
     * @return The row count across tables
     *
     * @see #count() The count of id of a specific table
     */
    @NotNull
    static NumericExpression<Long> rowCount() {
        return RowCounts.INSTANCE;
    }

    @NotNull
    static StringFactory string() {
        return ExpressionFactories.of(StringFactory.class);
    }

    @NotNull
    static NumericFactory numeric() {
        return ExpressionFactories.of(NumericFactory.class);
    }

    @NotNull
    static ComparableFactory comparable() {
        return ExpressionFactories.of(ComparableFactory.class);
    }

    @NotNull
    static AnyFactory any() {
        return ExpressionFactories.of(AnyFactory.class);
    }

    @NotNull
    static <T> Expression<T> nullValue(Class<T> type) {
        return any().nullValue(type);
    }

    @NotNull
    static <T1, T2> Expression<Tuple2<T1, T2>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2
    ) {
        return new Tuples.Expr2<>(expr1, expr2);
    }

    @NotNull
    static <T1, T2, T3> Expression<Tuple3<T1, T2, T3>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3
    ) {
        return new Tuples.Expr3<>(expr1, expr2, expr3);
    }

    @NotNull
    static <T1, T2, T3, T4> Expression<Tuple4<T1, T2, T3, T4>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4
    ) {
        return new Tuples.Expr4<>(expr1, expr2, expr3, expr4);
    }

    @NotNull
    static <T1, T2, T3, T4, T5> Expression<Tuple5<T1, T2, T3, T4, T5>> tuple(
            Expression<T1> expr1,
            Expression<T2> expr2,
            Expression<T3> expr3,
            Expression<T4> expr4,
            Expression<T5> expr5
    ) {
        return new Tuples.Expr5<>(expr1, expr2, expr3, expr4, expr5);
    }

    @NotNull
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

    @NotNull
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

    @NotNull
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

    @NotNull
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

        @NotNull
        StringExpression value(String value);

        @NotNull
        StringExpression sql(String sql);

        @NotNull
        StringExpression sql(String sql, Expression<?> expression, Object ... values);

        @NotNull
        StringExpression sql(String sql, Expression<?>[] expressions, Object ... values);

        @NotNull
        StringExpression sql(String sql, Consumer<SqlExpressionContext> block);

        @NotNull
        <C> SimpleCaseBuilder.Str<C> caseBuilder(C value);

        @NotNull
        <C> SimpleCaseBuilder.Str<C> caseBuilder(Expression<C> expression);

        @NotNull
        CaseBuilder.Str caseBuilder();
    }

    interface NumericFactory {

        @NotNull
        <N extends Number & Comparable<N>> NumericExpression<N> value(N value);

        @NotNull
        <N extends Number & Comparable<N>> NumericExpression<N> sql(Class<N> type, String sql);

        @NotNull
        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        @NotNull
        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        @NotNull
        <N extends Number & Comparable<N>> NumericExpression<N> sql(
                Class<N> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        @NotNull
        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, C value);

        @NotNull
        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, Expression<C> expression);

        @NotNull
        <N extends Number & Comparable<N>> CaseBuilder.Num<N> caseBuilder(Class<N> type);
    }

    /*
     * This class uses `T extends Comparable<?>`, not `T extends Comparable<T>`,
     * because not all types strictly implement the interface `Comparable<T>`,
     * such as `java.time.LocalDateTime`
     */
    interface ComparableFactory {

        @NotNull
        <T extends Comparable<?>> ComparableExpression<T> value(T value);

        @NotNull
        <T extends Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql);

        @NotNull
        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        @NotNull
        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        @NotNull
        <T extends Comparable<?>> ComparableExpression<T> sql(
                Class<T> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        @NotNull
        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T extends Comparable<?>> CaseBuilder.Cmp<T> caseBuilder(Class<T> type);
    }

    interface AnyFactory {

        @NotNull
        <T> Expression<T> value(T value);

        @NotNull
        <T> Expression<T> nullValue(Class<T> type);

        @NotNull
        <T> Expression<T> sql(Class<T> type, String sql);

        @NotNull
        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Expression<?> expression,
                Object ... values
        );

        @NotNull
        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Expression<?>[] expressions,
                Object ... values
        );

        @NotNull
        <T> Expression<T> sql(
                Class<T> type,
                String sql,
                Consumer<SqlExpressionContext> block
        );

        @NotNull
        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T> CaseBuilder<T> caseBuilder(Class<T> type);
    }
}
