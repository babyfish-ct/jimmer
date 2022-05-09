package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.StringExpression;

interface StringExpressionImplementor extends StringExpression, ExpressionImplementor<String> {

    @Override
    default Predicate like(String pattern, LikeMode likeMode) {
        return LikePredicate.of(this, pattern, false, likeMode);
    }

    @Override
    default Predicate ilike(String pattern, LikeMode likeMode) {
        return LikePredicate.of(this, pattern, true, likeMode);
    }

    @Override
    default StringExpression coalesce(String defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default CoalesceBuilder.Str coalesceBuilder() {
        return new CoalesceBuilder.Str(this);
    }
}
