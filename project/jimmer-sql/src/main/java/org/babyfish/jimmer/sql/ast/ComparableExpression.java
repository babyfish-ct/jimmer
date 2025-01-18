package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ComparableExpression<T extends Comparable<?>> extends Expression<T> {

    /**
     * Create `less than` predicate
     *
     * @param other The right operand which cannot be null
     * @return `less than` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate lt(@NotNull Expression<T> other);

    /**
     * Create `less than` predicate
     *
     * @param other The right operand which cannot be null
     * @return `less than` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate lt(@NotNull T other);

    /**
     * Create `less than` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other} is
     *                  neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `less than` predicate or null
     */
    @Nullable
    default Predicate ltIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? lt(other) : null;
    }

    /**
     * Create `less than` predicate when certain conditions are met.
     * @param other The right operand which can be null. If it is null or empty,
     *               returns null directly; otherwise, creates predicate
     * @return `less than` predicate or null
     */
    @Nullable
    default Predicate ltIf(@Nullable T other) {
        return ltIf(true, other);
    }

    /**
     * Create `less than or equal to` predicate
     *
     * @param other The right operand which cannot be null
     * @return `less than or equal to` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate le(@NotNull Expression<T> other);

    /**
     * Create `less than or equal to` predicate
     *
     * @param other The right operand which cannot be null
     * @return `less than or equal to` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate le(T other);

    /**
     * Create `less than or equal to` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other} is
     *                  neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `less than or equal to` predicate or null
     */
    @Nullable
    default Predicate leIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? le(other) : null;
    }

    /**
     * Create `less than or equal to` predicate when certain conditions are met.
     * @param other The right operand which can be null. If it is null or empty,
     *               returns null directly; otherwise, creates predicate
     * @return `less than or equal to` predicate or null
     */
    @Nullable
    default Predicate leIf(@Nullable T other) {
        return leIf(true, other);
    }

    /**
     * Create `greater than` predicate
     *
     * @param other The right operand which cannot be null
     * @return `greater than` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate gt(Expression<T> other);

    /**
     * Create `greater than` predicate
     *
     * @param other The right operand which cannot be null
     * @return `greater than` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate gt(T other);

    /**
     * Create `greater than` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other} is
     *                  neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `greater than` predicate or null
     */
    @Nullable
    default Predicate gtIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? gt(other) : null;
    }

    /**
     * Create `greater than` predicate when certain conditions are met.
     * @param other The right operand which can be null. If it is null or empty,
     *               returns null directly; otherwise, creates predicate
     * @return `greater than` predicate or null
     */
    @Nullable
    default Predicate gtIf(@Nullable T other) {
        return gtIf(true, other);
    }

    /**
     * Create `greater than or equal to` predicate
     *
     * @param other The right operand which cannot be null
     * @return `greater than or equal to` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate ge(Expression<T> other);

    /**
     * Create `greater than or equal to` predicate
     *
     * @param other The right operand which cannot be null
     * @return `greater than or equal to` predicate
     * @exception IllegalArgumentException The argument {@code other} is null
     */
    @NotNull
    Predicate ge(T other);

    /**
     * Create `greater than or equal to` predicate when certain conditions are met.
     * @param condition If this argument is true and the {@code other} is
     *                  neither null nor empty string, creates predicate;
     *                  otherwise, returns null
     * @param other The right operand which can be null. If it is null or empty string,
     *               returns null directly; otherwise, check if {@code condition}
     *               is true to decide whether to create predicate
     * @return `greater than or equal to` predicate or null
     */
    @Nullable
    default Predicate geIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? ge(other) : null;
    }

    /**
     * Create `greater than or equal to` predicate when certain conditions are met.
     * @param other The right operand which can be null. If it is null or empty,
     *               returns null directly; otherwise, creates predicate
     * @return `greater than or equal to` predicate or null
     */
    @Nullable
    default Predicate geIf(@Nullable T other) {
        return geIf(true, other);
    }

    /**
     * Create `between` predicate
     *
     * @param min The min bound which cannot be null
     * @param max The max bound which cannot be null
     *
     * @return `between` predicate
     * @exception IllegalArgumentException The argument {@code min} or {@code max} is null
     */
    @NotNull
    Predicate between(Expression<T> min, Expression<T> max);

    /**
     * Create `between` predicate
     *
     * @param min The min bound which cannot be null
     * @param max The max bound which cannot be null
     *
     * @return `between` predicate
     * @exception IllegalArgumentException The argument {@code min} or {@code max} is null
     */
    @NotNull
    Predicate between(T min, T max);

    /**
     * Create `between`, `less than or equal to` or `greater than or equal to` predicate when certain conditions are met.
     *
     * <ul>
     *     <li>If {@code condition} is false, returns null directly</li>
     *     <li>If {@code condition} is true and both {@code min} and {@code max} are null or empty string, returns null directly</li>
     *     <li>If {@code condition} is true and only {@code min} is null or empty string, returns {@code le(max)}</li>
     *     <li>If {@code condition} is true and only {@code max} is null or empty string, returns {@code ge(min)}</li>
     *     <li>If {@code condition} is true and none of {@code min} and {@code max} is null or empty string, creates `between` predicate</li>
     * </ul>
     * @param min The min bound which can be null
     * @param max The max bound which can be null
     * @return null, `between`, `less than or equal to` or `greater than or equal to` predicate
     */
    @Nullable
    default Predicate betweenIf(boolean condition, @Nullable T min, @Nullable T max) {
        if (!condition) {
            return null;
        }
        boolean noMin = min == null || "".equals(min);
        boolean noMax = max == null || "".equals(max);
        if (noMin && noMax) {
            return null;
        }
        if (noMin) {
            return le(max);
        }
        if (noMax) {
            return ge(min);
        }
        return between(min, max);
    }

    /**
     * Create `between`, `less than or equal to` or `greater than or equal to` predicate when certain conditions are met.
     *
     * <ul>
     *     <li>If both {@code min} and {@code max} are null or empty string, returns null directly</li>
     *     <li>If only {@code min} is null or empty string, returns {@code le(max)}</li>
     *     <li>If only {@code max} is null or empty string, returns {@code ge(min)}</li>
     *     <li>If none of {@code min} and {@code max} is null or empty string, create `between` predicate</li>
     * </ul>
     * @param min The min bound which can be null
     * @param max The max bound which can be null
     * @return null, `between`, `less than or equal to` or `greater than or equal to` predicate
     */
    @Nullable
    default Predicate betweenIf(@Nullable T min, @Nullable T max) {
        return betweenIf(true, min, max);
    }

    /**
     * Create `not between` predicate
     *
     * @param min The min bound which cannot be null
     * @param max The max bound which cannot be null
     *
     * @return `not between` predicate
     * @exception IllegalArgumentException The argument {@code min} or {@code max} is null
     */
    @NotNull
    Predicate notBetween(Expression<T> min, Expression<T> max);

    /**
     * Create `not between` predicate
     *
     * @param min The min bound which cannot be null
     * @param max The max bound which cannot be null
     *
     * @return `not between` predicate
     * @exception IllegalArgumentException The argument {@code min} or {@code max} is null
     */
    @NotNull
    Predicate notBetween(T min, T max);

    /**
     * Create `not between`, `greater than` or `less than` predicate when certain conditions are met.
     *
     * <ul>
     *     <li>If {@code condition} is false, returns null directly</li>
     *     <li>If {@code condition} is true and both {@code min} and {@code max} are null or empty string, returns null directly</li>
     *     <li>If {@code condition} is true and only {@code min} is null or empty string, returns {@code gt(max)}</li>
     *     <li>If {@code condition} is true and only {@code max} is null or empty string, returns {@code lt(min)}</li>
     *     <li>If {@code condition} is true and none of {@code min} and {@code max} is null or empty string, creates `not between` predicate</li>
     * </ul>
     * @param min The min bound which can be null
     * @param max The max bound which can be null
     * @return null, `between`, `greater than` or `less than` predicate
     */
    @Nullable
    default Predicate notBetweenIf(boolean condition, @Nullable T min, @Nullable T max) {
        if (!condition) {
            return null;
        }
        boolean noMin = min == null || "".equals(min);
        boolean noMax = max == null || "".equals(max);
        if (noMin && noMax) {
            return null;
        }
        if (noMin) {
            return gt(max);
        }
        if (noMax) {
            return lt(min);
        }
        return notBetween(min, max);
    }

    /**
     * Create `not between`, `greater than` or `less than` predicate when certain conditions are met.
     *
     * <ul>
     *     <li>If both {@code min} and {@code max} are null or empty string, returns null directly</li>
     *     <li>If only {@code min} is null or empty string, returns {@code gt(max)}</li>
     *     <li>If only {@code max} is null or empty string, returns {@code lt(min)}</li>
     *     <li>If none of {@code min} and {@code max} is null or empty string, creates `not between` predicate</li>
     * </ul>
     * @param min The min bound which can be null
     * @param max The max bound which can be null
     * @return null, `between`, `greater than` or `less than` predicate
     */
    @Nullable
    default Predicate notBetweenIf(@Nullable T min, @Nullable T max) {
        return notBetweenIf(true, min, max);
    }

    @NotNull
    ComparableExpression<T> min();

    @NotNull
    ComparableExpression<T> max();

    @Override
    @NotNull
    ComparableExpression<T> coalesce(T defaultValue);

    @Override
    @NotNull
    ComparableExpression<T> coalesce(Expression<T> defaultExpr);

    @Override
    @NotNull
    CoalesceBuilder.@NotNull Cmp<T> coalesceBuilder();
}
