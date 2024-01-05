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
    default Predicate lt(boolean condition, @Nullable T other) {
        return condition && other != null ? lt(other) : null;
    }

    @NotNull
    Predicate le(@NotNull Expression<T> other);

    @NotNull
    Predicate le(@NotNull T other);

    @Nullable
    default Predicate le(boolean condition, @Nullable T other) {
        return condition && other != null ? le(other) : null;
    }

    @NotNull
    Predicate gt(@NotNull Expression<T> other);

    @NotNull
    Predicate gt(@NotNull T other);

    @Nullable
    default Predicate gt(boolean condition, @Nullable T other) {
        return condition && other != null ? gt(other) : null;
    }

    @NotNull
    Predicate ge(@NotNull Expression<T> other);

    @NotNull
    Predicate ge(@NotNull T other);

    @Nullable
    default Predicate ge(boolean condition, @Nullable T other) {
        return condition && other != null ? ge(other) : null;
    }

    @NotNull
    Predicate between(@NotNull Expression<T> min, @NotNull Expression<T> max);

    @NotNull
    Predicate between(@NotNull T min, @NotNull T max);

    @Nullable
    default Predicate between(boolean condition, @Nullable T min, @Nullable T max) {
        if (!condition) {
            return null;
        }
        if (min == null && max == null) {
            return null;
        }
        if (min == null) {
            return le(max);
        }
        if (max == null) {
            return ge(min);
        }
        return between(min, max);
    }

    @NotNull
    Predicate notBetween(@NotNull Expression<T> min, @NotNull Expression<T> max);

    @NotNull
    Predicate notBetween(@NotNull T min, @NotNull T max);

    @Nullable
    default Predicate notBetween(boolean condition, @Nullable T min, @Nullable T max) {
        if (!condition) {
            return null;
        }
        if (min == null && max == null) {
            return null;
        }
        if (min == null) {
            return gt(max);
        }
        if (max == null) {
            return lt(min);
        }
        return notBetween(min, max);
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
