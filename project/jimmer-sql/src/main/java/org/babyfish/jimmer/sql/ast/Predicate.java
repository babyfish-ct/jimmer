package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.impl.CompositePredicate;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressionContext;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressions;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public interface Predicate extends Expression<Boolean> {

    /**
     * This function will be deleted in 1.0.0,
     * please use the static method {@link #and(Predicate...)}
     */
    @Deprecated
    @NewChain
    Predicate and(@Nullable Predicate other);

    /**
     * This function will be deleted in 1.0.0,
     * please use the static method {@link #or(Predicate...)}
     */
    @Deprecated
    @NewChain
    Predicate or(@Nullable Predicate other);

    /**
     * This function will be deleted in 1.0.0,
     * please use the static method {@link #not(Predicate)}
     */
    @Deprecated
    @NewChain
    Predicate not();

    @Nullable
    static Predicate and(Predicate ... predicates) {
        return CompositePredicate.and(predicates);
    }

    @Nullable
    static Predicate or(Predicate ... predicates) {
        return CompositePredicate.or(predicates);
    }
    
    @Nullable
    static Predicate not(@Nullable Predicate predicate) {
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
