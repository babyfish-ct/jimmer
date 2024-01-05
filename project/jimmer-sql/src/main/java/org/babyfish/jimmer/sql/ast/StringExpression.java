package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StringExpression extends ComparableExpression<String> {

    @NotNull
    default Predicate like(@NotNull String pattern) {
        return like(pattern, LikeMode.ANYWHERE);
    }

    @Nullable
    default Predicate like(boolean condition, @Nullable String pattern) {
        return condition && pattern != null && !pattern.isEmpty() ? like(pattern, LikeMode.ANYWHERE) : null;
    }

    @NotNull
    Predicate like(@NotNull String pattern, @NotNull LikeMode likeMode);

    @Nullable
    default Predicate like(boolean condition, @Nullable String pattern, @NotNull LikeMode likeMode) {
        return condition && pattern != null && (!pattern.isEmpty() || likeMode == LikeMode.EXACT) ?
                like(pattern, likeMode) :
                null;
    }

    @NotNull
    default Predicate ilike(@NotNull String pattern) {
        return ilike(pattern, LikeMode.ANYWHERE);
    }

    @Nullable
    default Predicate ilike(boolean condition, @Nullable String pattern) {
        return condition && pattern != null && !pattern.isEmpty() ? ilike(pattern, LikeMode.ANYWHERE) : null;
    }

    @NotNull
    Predicate ilike(@NotNull String pattern, @NotNull LikeMode likeMode);

    @Nullable
    default Predicate ilike(boolean condition, @Nullable String pattern, @NotNull LikeMode likeMode) {
        return condition && pattern != null && (!pattern.isEmpty() || likeMode == LikeMode.EXACT) ?
                ilike(pattern, likeMode) :
                null;
    }

    StringExpression upper();

    StringExpression lower();

    StringExpression concat(String ... others);

    @NotNull
    StringExpression concat(Expression<String> ... others);

    @Override
    @NotNull
    StringExpression coalesce(String defaultValue);

    @Override
    @NotNull
    StringExpression coalesce(Expression<String> defaultExpr);

    @Override
    @NotNull
    CoalesceBuilder.Str coalesceBuilder();
}
