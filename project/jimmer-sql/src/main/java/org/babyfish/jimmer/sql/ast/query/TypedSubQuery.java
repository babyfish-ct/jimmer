package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface TypedSubQuery<R> extends Expression<R> {

    Expression<R> all();

    Expression<R> any();

    Predicate exists();

    Predicate notExists();

    TypedSubQuery<R> union(TypedSubQuery<R> other);

    TypedSubQuery<R> unionAll(TypedSubQuery<R> other);

    TypedSubQuery<R> minus(TypedSubQuery<R> other);

    TypedSubQuery<R> intersect(TypedSubQuery<R> other);
}
