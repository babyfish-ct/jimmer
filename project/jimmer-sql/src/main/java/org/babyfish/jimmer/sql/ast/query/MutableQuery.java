package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.List;

public interface MutableQuery extends Sortable {

    @OldChain
    @Override
    MutableQuery orderBy(Expression<?>... expressions);

    @OldChain
    @Override
    default MutableQuery orderByIf(boolean condition, Expression<?>... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    @OldChain
    @Override
    MutableQuery orderBy(Order... orders);

    @OldChain
    @Override
    default MutableQuery orderByIf(boolean condition, Order... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @OldChain
    @Override
    MutableQuery orderBy(List<Order> orders);

    @OldChain
    @Override
    default MutableQuery orderByIf(boolean condition, List<Order> orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }

    @OldChain
    MutableQuery groupBy(Expression<?> ... expressions);

    @OldChain
    MutableQuery having(Predicate ... predicates);
}
