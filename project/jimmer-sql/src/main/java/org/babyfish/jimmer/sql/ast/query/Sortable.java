package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface Sortable extends Filterable {

    @OldChain
    @Override
    Sortable where(Predicate... predicates);

    @OldChain
    default Sortable orderBy(
            Expression<?> expression
    ) {
        return orderBy(
                expression,
                OrderMode.ASC,
                NullOrderMode.UNSPECIFIED
        );
    }

    @OldChain
    default Sortable orderBy(
            Expression<?> expression,
            OrderMode orderMode
    ) {
        return orderBy(
                expression,
                orderMode,
                NullOrderMode.UNSPECIFIED
        );
    }

    @OldChain
    Sortable orderBy(
            Expression<?> expression,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    );
}
