package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.selectable.SubSelectable;

import java.util.function.Supplier;

public interface MutableSubQuery extends MutableQuery, SubSelectable {

    @OldChain
    @Override
    MutableSubQuery where(Predicate... predicates);

    @OldChain
    @Override
    default MutableSubQuery whereIf(boolean condition, Predicate predicates) {
        if (condition) {
            where(predicates);
        }
        return this;
    }

    @OldChain
    @Override
    default MutableSubQuery whereIf(boolean condition, Supplier<Predicate> block) {
        if (condition) {
            where(block.get());
        }
        return this;
    }

    @OldChain
    @Override
    default MutableSubQuery orderBy(Expression<?> ... expressions) {
        return (MutableSubQuery) MutableQuery.super.orderBy(expressions);
    }

    @OldChain
    @Override
    default MutableSubQuery orderByIf(boolean condition, Expression<?>... expressions) {
        return (MutableSubQuery) MutableQuery.super.orderByIf(condition, expressions);
    }

    @OldChain
    @Override
    MutableSubQuery orderBy(Order ... orders);

    @OldChain
    @Override
    default MutableSubQuery orderByIf(boolean condition, Order... orders) {
        return (MutableSubQuery) MutableQuery.super.orderByIf(condition, orders);
    }

    @OldChain
    @Override
    MutableSubQuery groupBy(Expression<?>... expressions);

    @OldChain
    @Override
    MutableSubQuery having(Predicate... predicates);

    Predicate exists();

    Predicate notExists();
}
