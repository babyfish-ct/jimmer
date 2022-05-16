package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableQuery extends Filterable {

    default MutableQuery orderBy(
            Expression<?> expression
    ) {
        return orderBy(
                expression,
                OrderMode.ASC,
                NullOrderMode.UNSPECIFIED
        );
    }

    default MutableQuery orderBy(
            Expression<?> expression,
            OrderMode orderMode
    ) {
        return orderBy(
                expression,
                orderMode,
                NullOrderMode.UNSPECIFIED
        );
    }

    MutableQuery orderBy(
            Expression<?> expression,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    );

    MutableQuery groupBy(Expression<?> ... expressions);

    MutableQuery having(Predicate ... predicates);
}
