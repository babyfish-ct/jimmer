package org.babyfish.jimmer.sql.ast.impl;

abstract class AbstractPredicate extends AbstractExpression<Boolean> implements PredicateImplementor {

    @Override
    public final Class<Boolean> getType() {
        return Boolean.class;
    }
}
