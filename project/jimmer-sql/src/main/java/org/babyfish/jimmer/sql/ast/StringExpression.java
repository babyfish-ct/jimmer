package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StringExpression extends ComparableExpression<String> {

    /**
     * Returns a LIKE predicate for the given pattern with default match mode.
     *
     * <p>The pattern must not be null. The default match mode used is 
     * {@link LikeMode#ANYWHERE}, which matches the pattern anywhere in the value.
     *
     * @param pattern the pattern to match, must not be null
     * @return the LIKE predicate
     * @throws NullPointerException if the pattern is null
     */
    @NotNull
    default Predicate like(String pattern) {
        return like(pattern, LikeMode.ANYWHERE);
    }

    /**
     * Returns a LIKE predicate if the given condition is true and pattern is not null or empty.
     * The LIKE predicate matches values that contain the given pattern anywhere in the value (LikeMode.ANYWHERE).
     *
     * @param condition the condition to check
     * @param pattern the pattern to match in the LIKE predicate, may be null
     * @return a LIKE predicate if the condition is true and pattern is not null or empty, null otherwise
     */
    @Nullable
    default Predicate likeIf(boolean condition, @Nullable String pattern) {
        return condition && pattern != null && !pattern.isEmpty() ? 
                like(pattern, LikeMode.ANYWHERE) : 
                null;
    }

    /**
     * Returns a LIKE predicate if the given pattern is not null or empty.
     * The LIKE predicate matches values that contain the given pattern anywhere in the value (LikeMode.ANYWHERE).
     *
     * <p>This is an overload of the {@link #likeIf(boolean, String)} method that
     * passes a condition of true.
     *
     * @param pattern the pattern to match in the LIKE predicate, may be null
     * @return a LIKE predicate if the pattern is not null or empty, null otherwise
     */
    @Nullable
    default Predicate likeIf(@Nullable String pattern) {
        return likeIf(true, pattern);
    }

    /**
     * Returns a LIKE predicate with the given pattern and match mode.
     *
     * <p>Neither the pattern nor match mode may be null.
     *
     * @param pattern the pattern to match, must not be null
     * @param likeMode the mode determining how to match the pattern, must not be null
     * @return the LIKE predicate
     * @throws NullPointerException if pattern or likeMode is null
     */
    @NotNull
    Predicate like(String pattern, LikeMode likeMode);

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
    default Predicate likeIf(boolean condition, @Nullable String pattern, LikeMode likeMode) {
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
    default Predicate likeIf(@Nullable String pattern, LikeMode likeMode) {
        return likeIf(true, pattern, likeMode);
    }

    /**
     * Returns a case-insensitive like predicate for the given pattern with default match mode.
     *
     * <p>The pattern must not be null. The default match mode used is
     * {@link LikeMode#ANYWHERE}, which matches the pattern anywhere in the value.
     *
     * @param pattern the pattern to match, must not be null
     * @return the case-insensitive LIKE predicate
     * @throws NullPointerException if the pattern is null
     */
    @NotNull
    default Predicate ilike(String pattern) {
        return ilike(pattern, LikeMode.ANYWHERE);
    }

    /**
     * Create `case-insensitive like` expression when the argument `condition` is true
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
     * Create `case-insensitive like` expression when the argument `pattern` is neither null nor empty string.
     * @param pattern The pattern, If it is null or empty string, returns null directly,
     *                otherwise check create the expression
     * @return The `insensitively like` expression or null
     */
    @Nullable
    default Predicate ilikeIf(@Nullable String pattern) {
        return ilikeIf(true, pattern);
    }

    /**
     * Returns a case-insensitive LIKE predicate with the given pattern and match mode.
     *
     * <p>Neither the pattern nor like mode may be null,
     * otherwise {@code NullPointerException} will be thrown.
     *
     * @param pattern the pattern to match in the LIKE predicate, must not be null
     * @param likeMode the mode determining how to match the pattern, must not be null
     * @return the case-insensitive LIKE predicate
     * @throws NullPointerException if {@code pattern} or {@code likeMode} is null
     */
    @NotNull
    Predicate ilike(String pattern, LikeMode likeMode);

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
    default Predicate ilikeIf(boolean condition, @Nullable String pattern, LikeMode likeMode) {
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
    default Predicate ilikeIf(@Nullable String pattern, LikeMode likeMode) {
        return ilikeIf(true, pattern, likeMode);
    }

    StringExpression upper();

    StringExpression lower();

    /**
     * Returns a string with leading and trailing whitespace removed.
     * 
     * @return a trimmed version of this string expression
     */
    StringExpression trim();

    /**
     * Returns a string with leading whitespace removed.
     * 
     * @return a left-trimmed version of this string expression
     */
    StringExpression ltrim();

    /**
     * Returns a string with trailing whitespace removed.
     * 
     * @return a right-trimmed version of this string expression
     */
    StringExpression rtrim();

    /**
     * Returns the length of the string.
     * 
     * @return an expression representing the length of this string
     */
    NumericExpression<Integer> length();

    /**
     * Replaces all occurrences of a specified target string with a specified replacement string.
     *
     * @param target the string to be replaced
     * @param replacement the string to replace all occurrences of target
     * @return a new string expression with all occurrences of target replaced by replacement
     */
    StringExpression replace(String target, String replacement);

    /**
     * Returns a string with characters in reverse order.
     * For example, reverse("SQL Tutorial") returns "lairotuT LQS".
     * 
     * @return a new string expression with all characters in reverse order
     */
    StringExpression reverse();

    StringExpression concat(String ... others);

    @NotNull
    StringExpression concat(Expression<String> ... others);

    /**
     * Returns a substring starting from the specified position (1-based index).
     *
     * @param start the starting position (1-based index)
     * @return a substring of this string expression
     */
    StringExpression substring(int start);

    /**
     * Returns a substring starting from the specified position with the specified length.
     *
     * @param start the starting position (1-based index)
     * @param length the length of the substring
     * @return a substring of this string expression
     */
    StringExpression substring(int start, int length);

    /**
     * Returns a substring starting from the specified position (1-based index).
     *
     * @param start the starting position expression (1-based index)
     * @return a substring of this string expression
     */
    StringExpression substring(Expression<Integer> start);

    /**
     * Returns a substring starting from the specified position with the specified length.
     *
     * @param start the starting position expression (1-based index)
     * @param length the length expression of the substring
     * @return a substring of this string expression
     */
    StringExpression substring(Expression<Integer> start, Expression<Integer> length);

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
