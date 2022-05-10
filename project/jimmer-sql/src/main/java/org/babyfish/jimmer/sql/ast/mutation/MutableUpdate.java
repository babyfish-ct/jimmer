package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface MutableUpdate {

    <X> MutableUpdate set(Expression<X> path, X value);

    <X> MutableUpdate set(Expression<X> path, Expression<X> value);

    MutableUpdate where(Predicate ... predicates);
}
