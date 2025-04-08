package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface StringExpressionImplementor extends StringExpression, ComparableExpressionImplementor<String> {

    @Override
    default Class<String> getType() {
        return String.class;
    }

    @Override
    default @NotNull Predicate like(@NotNull String pattern, @NotNull LikeMode likeMode) {
        return LikePredicate.of(
                this,
                ParameterUtils.validate("like", "pattern", pattern),
                false,
                Objects.requireNonNull(likeMode, "`likeMode` cannot be null")
        );
    }

    @Override
    default @NotNull Predicate ilike(@NotNull String pattern, @NotNull LikeMode likeMode) {
        return LikePredicate.of(
                this,
                ParameterUtils.validate("ilike", "pattern", pattern),
                true,
                Objects.requireNonNull(likeMode, "`likeMode` cannot be null")
        );
    }

    @Override
    default StringExpression upper() {
        return new UpperExpression(this);
    }

    @Override
    default StringExpression lower() {
        return new LowerExpression(this);
    }

    @Override
    default StringExpression trim() {
        return new TrimExpression(this);
    }

    @Override
    default StringExpression ltrim() {
        return new LTrimExpression(this);
    }

    @Override
    default StringExpression rtrim() {
        return new RTrimExpression(this);
    }

    @Override
    default NumericExpression<Integer> length() {
        return new LengthExpression(this);
    }

    @Override
    default StringExpression concat(String... others) {
        return concat(
                Arrays.stream(others)
                        .filter(it -> it != null && !it.isEmpty())
                        .map(Literals::string)
                        .toArray(StringExpression[]::new)
        );
    }

    @Override
    default @NotNull StringExpression concat(Expression<String>... others) {
        List<Expression<String>> exprs =
                Arrays.stream(others)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        if (exprs.isEmpty()) {
            return this;
        }
        return new ConcatExpression(this, exprs);
    }

    @Override
    default StringExpression substring(int start) {
        return new SubstringExpression(this, Literals.number(start));
    }

    @Override
    default StringExpression substring(int start, int length) {
        return new SubstringExpression(this, Literals.number(start), Literals.number(length));
    }

    @Override
    default StringExpression substring(Expression<Integer> start) {
        return new SubstringExpression(this, start);
    }

    @Override
    default StringExpression substring(Expression<Integer> start, Expression<Integer> length) {
        return new SubstringExpression(this, start, length);
    }

    @Override
    default @NotNull StringExpression coalesce(String defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default @NotNull StringExpression coalesce(Expression<String> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default CoalesceBuilder.@NotNull Str coalesceBuilder() {
        return new CoalesceBuilder.Str(this);
    }

    @Override
    default StringExpression replace(String target, String replacement) {
        return new ReplaceExpression(
            this, 
            Objects.requireNonNull(target, "target cannot be null"),
            Objects.requireNonNull(replacement, "replacement cannot be null")
        );
    }

    @Override
    default StringExpression reverse() {
        return new ReverseExpression(this);
    }

    @Override
    default StringExpression lpad(int length, String padString) {
        return lpad(Literals.number(length), padString);
    }
    
    @Override
    default StringExpression lpad(Expression<Integer> length, String padString) {
        return new LPadExpression(
            this,
            Objects.requireNonNull(length, "length cannot be null"),
            Objects.requireNonNull(padString, "padString cannot be null")
        );
    }
    
    @Override
    default StringExpression rpad(int length, String padString) {
        return rpad(Literals.number(length), padString);
    }
    
    @Override
    default StringExpression rpad(Expression<Integer> length, String padString) {
        return new RPadExpression(
            this,
            Objects.requireNonNull(length, "length cannot be null"),
            Objects.requireNonNull(padString, "padString cannot be null")
        );
    }

    @Override
    default NumericExpression<Integer> position(String substring) {
        return new PositionExpression(
            Objects.requireNonNull(substring, "substring cannot be null"),
            this
        );
    }
    
    @Override
    default NumericExpression<Integer> locate(String substring, int start) {
        return locate(substring, Literals.number(start));
    }
    
    @Override
    default NumericExpression<Integer> locate(String substring, Expression<Integer> start) {
        return new LocateExpression(
            Objects.requireNonNull(substring, "substring cannot be null"),
            this,
            start
        );
    }
    
    @Override
    default StringExpression left(int length) {
        return left(Literals.number(length));
    }
    
    @Override
    default StringExpression left(Expression<Integer> length) {
        return new LeftExpression(
            this,
            Objects.requireNonNull(length, "length cannot be null")
        );
    }
    
    @Override
    default StringExpression right(int length) {
        return right(Literals.number(length));
    }
    
    @Override
    default StringExpression right(Expression<Integer> length) {
        return new RightExpression(
            this,
            Objects.requireNonNull(length, "length cannot be null")
        );
    }
    
    @Override
    default StringExpression repeat(int count) {
        return repeat(Literals.number(count));
    }
    
    @Override
    default StringExpression repeat(Expression<Integer> count) {
        return new RepeatExpression(
            this,
            Objects.requireNonNull(count, "count cannot be null")
        );
    }
}
