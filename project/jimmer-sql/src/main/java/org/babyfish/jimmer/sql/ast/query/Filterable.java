package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface Filterable {

    Filterable where(Predicate...predicates);

    default Filterable orderBy(
            Expression<?> expression
    ) {
        return orderBy(
                expression,
                OrderMode.ASC,
                NullOrderMode.UNSPECIFIED
        );
    }

    default Filterable orderBy(
            Expression<?> expression,
            OrderMode orderMode
    ) {
        return orderBy(
                expression,
                orderMode,
                NullOrderMode.UNSPECIFIED
        );
    }

    Filterable orderBy(
            Expression<?> expression,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    );
}
