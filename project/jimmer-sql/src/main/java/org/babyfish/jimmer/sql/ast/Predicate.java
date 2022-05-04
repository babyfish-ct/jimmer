package org.babyfish.jimmer.sql.ast;

import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

public interface Predicate extends Expression<Boolean> {

    Predicate and(Predicate other);

    Predicate or(Predicate other);

    Predicate not();

    static Predicate exists(TypedSubQuery<?> subQuery) {
        throw new RuntimeException();
    }

    static Predicate notExists(TypedSubQuery<?> subQuery) {
        throw new RuntimeException();
    }

    static Predicate exists(MutableSubQuery subQuery) {
        throw new RuntimeException();
    }

    static Predicate notExists(MutableSubQuery subQuery) {
        throw new RuntimeException();
    }
}
