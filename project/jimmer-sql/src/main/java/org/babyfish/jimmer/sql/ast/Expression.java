package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.util.RowCounts;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
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
     * Create `nullable in` predicate
     *
     * <p>
     *     You can use `(a, b) in ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) in ((1, 2), (5, 6)) or a = 3 and b is null or a = 7 and b is null`
     * </p>
     *
     * @param values A collection which cannot be null
     * @return `in` predicate
     * @exception NullPointerException The argument {@code values} is null
     */
    @NotNull
    Predicate nullableIn(Collection<T> values);

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
     * Create `nullable-in` predicate when certain conditions are met.
     *
     * <p>
     *     You can use `(a, b) in ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) in ((1, 2), (5, 6)) or a = 3 and b is null or a = 7 and b is null`
     * </p>
     *
     * @param condition If this argument is true and the {@code other} is not null, creates predicate;
     *                  otherwise, returns null
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate nullableInIf(boolean condition, @Nullable Collection<T> values) {
        return condition && values != null ? nullableIn(values) : null;
    }

    /**
     * Create `in` predicate when certain conditions are met.
     *
     * <p>
     *     You can use `(a, b) in ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) in ((1, 2), (5, 6)) or a = 3 and b is null or a = 7 and b is null`
     * </p>
     *
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `in` predicate or null
     */
    @Nullable
    default Predicate nullableInIf(@Nullable Collection<T> values) {
        return nullableInIf(true, values);
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
     * Create `nullable not in` predicate
     *
     * <p>
     *     You can use `(a, b) not ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) not in ((1, 2), (5, 6)) and (a &lt;&gt; 3 or b is not null) and (a &lt;&gt; 7 or b is not null)`
     * </p>
     *
     * @param values A collection which cannot be null
     * @return `in` predicate
     * @exception NullPointerException The argument {@code values} is null
     */
    @NotNull
    Predicate nullableNotIn(Collection<T> values);

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
     * Create `nullable not in` predicate when certain conditions are met.
     *
     * <p>
     *     You can use `(a, b) not ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) not in ((1, 2), (5, 6)) and (a &lt;&gt; 3 or b is not null) and (a &lt;&gt; 7 or b is not null)`
     * </p>
     *
     * @param condition If this argument is true and the {@code values} is not null, creates predicate;
     *                  otherwise, returns null
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate nullableNotInIf(boolean condition, @Nullable Collection<T> values) {
        return condition && values != null ? nullableNotIn(values) : null;
    }

    /**
     * Create `nullable not in` predicate when certain conditions are met.
     *
     * <p>
     *     You can use `(a, b) not ((1, 2), (3, null), (5, 6), (7, null))`,
     *     it will be automatically translated to
     *     `(a, b) not in ((1, 2), (5, 6)) and (a &lt;&gt; 3 or b is not null) and (a &lt;&gt; 7 or b is not null)`
     * </p>
     *
     * @param values The right operand which can be null. If it is null,
     *               returns null directly; otherwise, creates predicate
     * @return `not in` predicate or null
     */
    @Nullable
    default Predicate nullableNotInIf(@Nullable Collection<T> values) {
        return nullableNotInIf(true, values);
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

    @SuppressWarnings("unchecked")
    default Predicate expressionIn(Collection<Expression<T>> operands) {
        return new InExpressionCollectionPredicate(
                false,
                this,
                (Collection<Expression<?>>) (Collection<?>)operands
        );
    }

    @SuppressWarnings("unchecked")
    default Predicate expressionNotIn(Collection<Expression<T>> operands) {
        return new InExpressionCollectionPredicate(
                true,
                this,
                (Collection<Expression<?>>) (Collection<?>)operands
        );
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

    static DateFactory date() {
        return ExpressionFactories.of(DateFactory.class);
    }

    static TemporalFactory temporal() {
        return ExpressionFactories.of(TemporalFactory.class);
    }

    @NotNull
    static AnyFactory any() {
        return ExpressionFactories.of(AnyFactory.class);
    }

    static StringExpression value(String value) {
        return Literals.string(value);
    }

    static <N extends Number & Comparable<N>> NumericExpression<N> value(N value) {
        return Literals.number(value);
    }

    @NotNull
    static <T extends Date & Comparable<Date>> DateExpression<T> value(@NotNull T value) {
        return Literals.date(value);
    }

    @NotNull
    static <T extends Temporal & Comparable<?>> TemporalExpression<T> value(@NotNull T value) {
        return Literals.temporal(value);
    }

    static <T extends Comparable<?>> ComparableExpression<T> value(T value) {
        return Literals.comparable(value);
    }

    static <T> Expression<T> value(T value) {
        return Literals.any(value);
    }

    @NotNull
    static <T> Expression<T> nullValue(Class<T> type) {
        return new NullExpression<>(type);
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

        /**
         * @deprecated Please use {@link Expression#value(String)}
         */
        @Deprecated
        @NotNull
        default StringExpression value(String value) {
            return Expression.value(value);
        }

        @NotNull
        NativeBuilder.Str sqlBuilder(String sql);

        default StringExpression sql(String sql) {
            return sqlBuilder(sql).build();
        }

        default StringExpression sql(String sql, Expression<?> ... expressions) {
            NativeBuilder.Str builder = sqlBuilder(sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default StringExpression sql(String sql, Consumer<NativeContext> block) {
            NativeBuilder.Str builder = sqlBuilder(sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C> SimpleCaseBuilder.Str<C> caseBuilder(C value);

        @NotNull
        <C> SimpleCaseBuilder.Str<C> caseBuilder(Expression<C> expression);

        @NotNull
        CaseBuilder.Str caseBuilder();
    }

    interface NumericFactory {

        /**
         * @deprecated Please use {@link Expression#value(Number)}
         */
        @Deprecated
        @NotNull
        default <N extends Number & Comparable<N>> NumericExpression<N> value(N value) {
            return Expression.value(value);
        }

        @NotNull
        <N extends Number & Comparable<N>> NativeBuilder.Num<N> sqlBuilder(Class<N> type, String sql);

        default <N extends Number & Comparable<N>> NumericExpression<N> sql(Class<N> type, String sql) {
            return sqlBuilder(type, sql).build();
        }

        default <N extends Number & Comparable<N>> NumericExpression<N> sql(Class<N> type, String sql, Expression<?> ... expressions) {
            NativeBuilder.Num<N> builder = sqlBuilder(type, sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default <N extends Number & Comparable<N>> NumericExpression<N> sql(Class<N> type, String sql, Consumer<NativeContext> block) {
            NativeBuilder.Num<N> builder = sqlBuilder(type, sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, C value);

        @NotNull
        <C, N extends Number & Comparable<N>> SimpleCaseBuilder.Num<C, N> caseBuilder(Class<N> type, Expression<C> expression);

        @NotNull
        <N extends Number & Comparable<N>> CaseBuilder.Num<N> caseBuilder(Class<N> type);
    }

    /*
     * This class uses `T extends Comparable<?>`, not `T extends Comparable<T>`,
     * because not all types strictly implement the interface `Comparable<T>`
     */
    interface ComparableFactory {

        /**
         * @deprecated Please use {@link Expression#value(Comparable)}
         */
        @Deprecated
        @NotNull
        default <T extends Comparable<?>> ComparableExpression<T> value(T value) {
            return Expression.value(value);
        }

        @NotNull
        <T extends Comparable<?>> NativeBuilder.Cmp<T> sqlBuilder(Class<T> type, String sql);

        default <T extends Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql) {
            return sqlBuilder(type, sql).build();
        }

        default <T extends Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql, Expression<?> ... expressions) {
            NativeBuilder.Cmp<T> builder = sqlBuilder(type, sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default <T extends Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql, Consumer<NativeContext> block) {
            NativeBuilder.Cmp<T> builder = sqlBuilder(type, sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T extends Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T extends Comparable<?>> CaseBuilder.Cmp<T> caseBuilder(Class<T> type);
    }

    interface DateFactory {

        /**
         * @deprecated Please use {@link Expression#value(Date)}
         */
        @Deprecated
        default <T extends Date & Comparable<Date>> DateExpression<T> value(T value) {
            return Expression.value(value);
        }

        @NotNull
        <T extends Date & Comparable<Date>> NativeBuilder.Dt<T> sqlBuilder(Class<T> type, String sql);

        default <T extends Date & Comparable<Date>> ComparableExpression<T> sql(Class<T> type, String sql) {
            return sqlBuilder(type, sql).build();
        }

        default <T extends Date & Comparable<Date>> ComparableExpression<T> sql(Class<T> type, String sql, Expression<?> ... expressions) {
            NativeBuilder.Dt<T> builder = sqlBuilder(type, sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default <T extends Date & Comparable<Date>> ComparableExpression<T> sql(Class<T> type, String sql, Consumer<NativeContext> block) {
            NativeBuilder.Dt<T> builder = sqlBuilder(type, sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C, T extends Date & Comparable<Date>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T extends Date & Comparable<Date>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T extends Date & Comparable<Date>> CaseBuilder.Cmp<T> caseBuilder(Class<T> type);
    }

    interface TemporalFactory {

        /**
         * @deprecated Please use {@link Expression#value(Temporal)}
         */
        @Deprecated
        default <T extends Temporal & Comparable<?>> TemporalExpression<T> value(T value) {
            return Expression.value(value);
        }

        @NotNull
        <T extends Temporal & Comparable<?>> NativeBuilder.Tp<T> sqlBuilder(Class<T> type, String sql);

        default <T extends Temporal & Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql) {
            return sqlBuilder(type, sql).build();
        }

        default <T extends Temporal & Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql, Expression<?> ... expressions) {
            NativeBuilder.Tp<T> builder = sqlBuilder(type, sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default <T extends Temporal & Comparable<?>> ComparableExpression<T> sql(Class<T> type, String sql, Consumer<NativeContext> block) {
            NativeBuilder.Tp<T> builder = sqlBuilder(type, sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C, T extends Temporal & Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T extends Temporal & Comparable<?>> SimpleCaseBuilder.Cmp<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T extends Temporal & Comparable<?>> CaseBuilder.Cmp<T> caseBuilder(Class<T> type);
    }

    interface AnyFactory {

        /**
         * @deprecated Please use {@link Expression#value(Object)}
         */
        @Deprecated
        @NotNull
        default <T> Expression<T> value(T value) {
            return Expression.value(value);
        }

        @NotNull
        <T> NativeBuilder<T> sqlBuilder(Class<T> type, String sql);

        default <T> Expression<T> sql(Class<T> type, String sql) {
            return sqlBuilder(type, sql).build();
        }

        default <T> Expression<T> sql(Class<T> type, String sql, Expression<?> ... expressions) {
            NativeBuilder<T> builder = sqlBuilder(type, sql);
            for (Expression<?> expression : expressions) {
                builder.expression(expression);
            }
            return builder.build();
        }

        default <T> Expression<T> sql(Class<T> type, String sql, Consumer<NativeContext> block) {
            NativeBuilder<T> builder = sqlBuilder(type, sql);
            block.accept(builder);
            return builder.build();
        }

        @NotNull
        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, C value);

        @NotNull
        <C, T> SimpleCaseBuilder<C, T> caseBuilder(Class<T> type, Expression<C> expression);

        @NotNull
        <T> CaseBuilder<T> caseBuilder(Class<T> type);
    }
}
