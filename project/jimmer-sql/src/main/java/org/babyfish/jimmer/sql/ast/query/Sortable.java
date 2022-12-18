package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

import java.util.function.Supplier;

public interface Sortable extends Filterable {

    @OldChain
    @Override
    Sortable where(Predicate... predicates);

    @OldChain
    @Override
    default Sortable whereIf(boolean condition, Predicate predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }

    @OldChain
    @Override
    default Sortable whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

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
    default Sortable orderByIf(boolean condition, Expression<?> ... expressions) {
        if (condition) {
            orderBy(expressions);
        }
        return this;
    }

    @OldChain
    Sortable orderBy(Order ... orders);

    @OldChain
    default Sortable orderByIf(boolean condition, Order ... orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }
}
