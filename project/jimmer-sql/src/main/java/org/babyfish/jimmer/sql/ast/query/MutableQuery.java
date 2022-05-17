package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface MutableQuery extends Filterable {

    @OldChain
    default MutableQuery orderBy(
            Expression<?> expression
    ) {
        return orderBy(
                expression,
                OrderMode.ASC,
                NullOrderMode.UNSPECIFIED
        );
    }

    @OldChain
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

    @OldChain
    MutableQuery orderBy(
            Expression<?> expression,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    );

    @OldChain
    MutableQuery groupBy(Expression<?> ... expressions);

    @OldChain
    MutableQuery having(Predicate ... predicates);
}
