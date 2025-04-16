package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface Predicate extends Expression<Boolean> {

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
        return ((PredicateImplementor)predicate).not();
    }

    static NativeBuilder.Prd sqlBuilder(String sql) {
        return NativeBuilderImpl.predicate(sql);
    }

    static Predicate sql(String sql) {
        return sqlBuilder(sql).build();
    }

    static Predicate sql(String sql, Expression<?> ... expressions) {
        NativeBuilder.Prd builder = sqlBuilder(sql);
        for (Expression<?> expression : expressions) {
            builder.expression(expression);
        }
        return builder.build();
    }

    static Predicate sql(String sql, Consumer<NativeContext> block) {
        NativeBuilder.Prd builder = sqlBuilder(sql);
        block.accept(builder);
        return builder.build();
    }
}
