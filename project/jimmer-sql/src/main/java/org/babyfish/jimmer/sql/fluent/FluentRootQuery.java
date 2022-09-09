package org.babyfish.jimmer.sql.fluent;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface FluentRootQuery<T extends Table<?>> extends FluentFilterable, RootSelectable<T> {

    @Override
    FluentRootQuery<T> where(Predicate... predicates);

    FluentRootQuery<T> groupBy(Expression<?>... expressions);

    FluentRootQuery<T> having(Predicate... predicates);

    FluentRootQuery<T> orderBy(Expression<?>... expressions);

    FluentRootQuery<T> orderBy(Order... orders);
}
