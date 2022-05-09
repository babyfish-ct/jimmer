package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface TypedSubQuery<R> extends Expression<R> {

    Expression<R> all();

    Expression<R> any();

    Predicate exists();

    Predicate notExists();
}
