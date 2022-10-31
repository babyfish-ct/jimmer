package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.impl.CompositePredicate;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressionContext;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressions;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public interface Predicate extends Expression<Boolean> {

    @NewChain
    Predicate and(Predicate other);

    @NewChain
    Predicate or(Predicate other);

    @NewChain
    Predicate not();

    static Predicate and(Predicate ... predicates) {
        return CompositePredicate.and(predicates);
    }

    static Predicate or(Predicate ... predicates) {
        return CompositePredicate.or(predicates);
    }

    static Predicate not(Predicate predicate) {
        if (predicate == null) {
            return null;
        }
        return predicate.not();
    }

    static Predicate sql(String sql) {
        return SqlExpressions.of(Boolean.class, sql, null);
    }

    static Predicate sql(String sql, Expression<?> expression, Object ... values) {
        return SqlExpressions.of(Boolean.class, sql, new Expression[] { expression }, values);
    }

    static Predicate sql(String sql, Expression<?>[] expressions, Object ... values) {
        return SqlExpressions.of(Boolean.class, sql, expressions, values);
    }

    static Predicate sql(String sql, Consumer<SqlExpressionContext> block) {
        return SqlExpressions.of(Boolean.class, sql, block);
    }
}
