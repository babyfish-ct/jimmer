package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

interface StringExpressionImplementor extends StringExpression, ComparableExpressionImplementor<String> {

    @Override
    default Class<String> getType() {
        return String.class;
    }

    @Override
    @Nullable
    default Predicate like(String pattern, LikeMode likeMode) {
        return LikePredicate.of(this, pattern, false, likeMode);
    }

    @Override
    @Nullable
    default Predicate ilike(String pattern, LikeMode likeMode) {
        return LikePredicate.of(this, pattern, true, likeMode);
    }

    @Override
    default StringExpression upper() {
        return new UpperExpression(this);
    }

    @Override
    default StringExpression lower() {
        return new LowerExpression(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    default StringExpression concat(String... others) {
        return concat(
                Arrays.stream(others)
                        .filter(it -> it != null && !it.isEmpty())
                        .map(Literals::string)
                        .toArray(Expression[]::new)
        );
    }

    @Override
    default StringExpression concat(Expression<String>... others) {
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
    default StringExpression coalesce(String defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default StringExpression coalesce(Expression<String> defaultExpr) {
        return coalesceBuilder().or(defaultExpr).build();
    }

    @Override
    default CoalesceBuilder.Str coalesceBuilder() {
        return new CoalesceBuilder.Str(this);
    }
}
