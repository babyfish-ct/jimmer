package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

public interface Sortable extends Filterable {

    @OldChain
    @Override
    Sortable where(Predicate... predicates);

    @OldChain
    default Sortable orderBy(Expression<?> ... expressions) {
        Order[] orders = new Order[expressions.length];
        for (int i = orders.length - 1; i >= 0; --i) {
            Expression<?> expression = expressions[i];
            if (expression != null) {
                orders[i] = new Order(expression, OrderMode.ASC, NullOrderMode.UNSPECIFIED);
            }
        }
        return orderBy(orders);
    }

    @OldChain
    Sortable orderBy(Order ... orders);
}
