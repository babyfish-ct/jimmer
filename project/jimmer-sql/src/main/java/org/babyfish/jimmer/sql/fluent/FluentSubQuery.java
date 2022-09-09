package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable;

public interface FluentSubQuery extends FluentFilterable, SubSelectable {

    @Override
    FluentSubQuery where(Predicate... predicates);

    FluentSubQuery groupBy(Expression<?>... expressions);

    FluentSubQuery having(Predicate... predicates);

    FluentSubQuery orderBy(Expression<?>... expressions);

    FluentSubQuery orderBy(Order... orders);

    Predicate exists();

    Predicate notExists();
}
