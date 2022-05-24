package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressionContext;
import org.babyfish.jimmer.sql.ast.impl.SqlExpressions;

import java.util.function.Consumer;

public interface Predicate extends Expression<Boolean> {

    @NewChain
    Predicate and(Predicate other);

    @NewChain
    Predicate or(Predicate other);

    @NewChain
    Predicate not();

    static Predicate sql(String sql) {
        return sql(sql, null);
    }

    static Predicate sql(String sql, Consumer<SqlExpressionContext> block) {
        return SqlExpressions.of(Boolean.class, sql, block);
    }
}
