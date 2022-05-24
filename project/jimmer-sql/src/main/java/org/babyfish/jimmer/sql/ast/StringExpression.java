package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.CoalesceBuilder;

public interface StringExpression extends Expression<String> {

    default Predicate like(String pattern) {
        return like(pattern, LikeMode.ANYWHERE);
    }

    Predicate like(String pattern, LikeMode likeMode);

    default Predicate ilike(String pattern) {
        return ilike(pattern, LikeMode.ANYWHERE);
    }

    Predicate ilike(String pattern, LikeMode likeMode);

    StringExpression concat(String ... others);

    StringExpression concat(Expression<String> ... others);

    @Override
    StringExpression coalesce(String defaultValue);

    @Override
    StringExpression coalesce(Expression<String> defaultExpr);

    @Override
    CoalesceBuilder.Str coalesceBuilder();
}
