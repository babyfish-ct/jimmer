package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ComparableExpression<T extends Comparable<?>> extends Expression<T> {

    @NotNull
    Predicate lt(@NotNull Expression<T> other);

    @NotNull
    Predicate lt(@NotNull T other);

    @Nullable
    default Predicate ltIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? lt(other) : null;
    }

    @Nullable
    default Predicate ltIf(@Nullable T other) {
        return ltIf(true, other);
    }

    @NotNull
    Predicate le(@NotNull Expression<T> other);

    @NotNull
    Predicate le(T other);

    @Nullable
    default Predicate leIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? le(other) : null;
    }

    @Nullable
    default Predicate leIf(@Nullable T other) {
        return leIf(true, other);
    }

    @NotNull
    Predicate gt(Expression<T> other);

    @NotNull
    Predicate gt(T other);

    @Nullable
    default Predicate gtIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? gt(other) : null;
    }

    @Nullable
    default Predicate gtIf(@Nullable T other) {
        return gtIf(true, other);
    }

    @NotNull
    Predicate ge(Expression<T> other);

    @NotNull
    Predicate ge(T other);

    @Nullable
    default Predicate geIf(boolean condition, @Nullable T other) {
        return condition && other != null && !"".equals(other) ? ge(other) : null;
    }

    @Nullable
    default Predicate geIf(@Nullable T other) {
        return geIf(true, other);
    }

    @NotNull
    Predicate between(Expression<T> min, Expression<T> max);

    @NotNull
    Predicate between(T min, T max);

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

    @Nullable
    default Predicate betweenIf(@Nullable T min, @Nullable T max) {
        return betweenIf(true, min, max);
    }

    @NotNull
    Predicate notBetween(Expression<T> min, Expression<T> max);

    @NotNull
    Predicate notBetween(T min, T max);

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

    @Nullable
    default Predicate notBetweenIf(@Nullable T min, @Nullable T max) {
        return notBetweenIf(true, min, max);
    }

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
