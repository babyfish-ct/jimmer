package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;

import java.util.List;
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
    Sortable orderBy(Expression<?> ... expressions);

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

    @OldChain
    Sortable orderBy(List<Order> orders);

    @OldChain
    default Sortable orderByIf(boolean condition, List<Order> orders) {
        if (condition) {
            orderBy(orders);
        }
        return this;
    }
}
