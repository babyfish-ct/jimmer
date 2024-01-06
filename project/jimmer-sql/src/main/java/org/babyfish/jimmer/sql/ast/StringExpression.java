package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StringExpression extends ComparableExpression<String> {
    
    @NotNull
    default Predicate like(@NotNull String pattern) {
        return like(pattern, LikeMode.ANYWHERE);
    }

    /**
     * Create `like` expression when the argument `condition` is true
     * and the argument `pattern` is neither null nor empty string.
     * @param condition The condition, if it is false, returns null directly, 
     *                  otherwise check the other parameter `pattern`
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check the other parameter `condition`
     * @return The `like` expression or null
     */
    @Nullable
    default Predicate likeIf(boolean condition, @Nullable String pattern) {
        return condition && pattern != null && !pattern.isEmpty() ? 
                like(pattern, LikeMode.ANYWHERE) : 
                null;
    }

    /**
     * Create `like` expression when the argument `pattern` is neither null nor empty string.
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check create the expression
     * @return The `like` expression or null
     */
    @Nullable
    default Predicate likeIf(@Nullable String pattern) {
        return likeIf(true, pattern);
    }
    
    @NotNull
    Predicate like(@NotNull String pattern, @NotNull LikeMode likeMode);

    /**
     * Create `like` expression when the argument `condition` is true
     * and the argument `pattern` is neither null nor empty string.
     * @param condition The condition, if it is false, returns null directly, 
     *                  otherwise check the other parameter `pattern`
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check the other parameter `condition`
     * @param likeMode The like mode which can be {@link LikeMode#ANYWHERE}, 
     *                  {@link LikeMode#START}, {@link LikeMode#END} or {@link LikeMode#EXACT}
     * @return The `like` expression or null
     */
    @Nullable
    default Predicate likeIf(boolean condition, @Nullable String pattern, @NotNull LikeMode likeMode) {
        return condition && pattern != null && !pattern.isEmpty() ?
                like(pattern, likeMode) :
                null;
    }

    /**
     * Create `like` expression when the argument `pattern` is neither null nor empty string.
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check create the expression
     * @param likeMode The like mode which can be {@link LikeMode#ANYWHERE},
     *                 {@link LikeMode#START}, {@link LikeMode#END} or {@link LikeMode#EXACT}
     * @return The `like` expression or null
     */
    @Nullable
    default Predicate likeIf(@Nullable String pattern, @NotNull LikeMode likeMode) {
        return likeIf(true, pattern, likeMode);
    }

    @NotNull
    default Predicate ilike(@NotNull String pattern) {
        return ilike(pattern, LikeMode.ANYWHERE);
    }

    /**
     * Create `insensitively like` expression when the argument `condition` is true
     * and the argument `pattern` is neither null nor empty string.
     * @param condition The condition, if it is false, returns null directly, 
     *                  otherwise check the other parameter `pattern`
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check the other parameter `condition`
     * @return The `insensitively like` expression or null
     */
    @Nullable
    default Predicate ilikeIf(boolean condition, @Nullable String pattern) {
        return condition && pattern != null && !pattern.isEmpty() ? 
                ilike(pattern, LikeMode.ANYWHERE) : 
                null;
    }

    /**
     * Create `insensitively like` expression when the argument `pattern` is neither null nor empty string.
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check create the expression
     * @return The `insensitively like` expression or null
     */
    @Nullable
    default Predicate ilikeIf(@Nullable String pattern) {
        return ilikeIf(true, pattern);
    }

    @NotNull
    Predicate ilike(@NotNull String pattern, @NotNull LikeMode likeMode);

    /**
     * Create `insensitively like` expression when the argument `condition` is true
     * and the argument `pattern` is neither null nor empty string.
     * @param condition The condition, if it is false, returns null directly, 
     *                  otherwise check the other parameter `pattern`
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check the other parameter `condition`
     * @param likeMode The like mode which can be {@link LikeMode#ANYWHERE}, 
     *                  {@link LikeMode#START}, {@link LikeMode#END} or {@link LikeMode#EXACT}
     * @return The `insensitively like` expression or null
     */
    @Nullable
    default Predicate ilikeIf(boolean condition, @Nullable String pattern, @NotNull LikeMode likeMode) {
        return condition && pattern != null && !pattern.isEmpty() ?
                ilike(pattern, likeMode) :
                null;
    }

    /**
     * Create `insensitively like` expression when the argument `pattern` is neither null nor empty string.
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check create the expression
     * @param likeMode The like mode which can be {@link LikeMode#ANYWHERE},
     *                 {@link LikeMode#START}, {@link LikeMode#END} or {@link LikeMode#EXACT}
     * @return The `insensitively like` expression or null
     */
    @Nullable
    default Predicate ilikeIf(@Nullable String pattern, @NotNull LikeMode likeMode) {
        return ilikeIf(true, pattern, likeMode);
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
