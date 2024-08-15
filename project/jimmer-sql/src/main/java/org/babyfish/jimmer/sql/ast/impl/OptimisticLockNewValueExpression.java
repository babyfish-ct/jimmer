package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.jetbrains.annotations.NotNull;

class OptimisticLockNewValueExpression<V>
        extends AbstractExpression<V> {

    private final ImmutableProp prop;

    OptimisticLockNewValueExpression(TypedProp.Scalar<?, V> prop) {
        this.prop = prop.unwrap();
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return false;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        if (!(builder instanceof BatchSqlBuilder)) {
            throw new IllegalStateException(
                    "The \"" +
                            OptimisticLockNewValueExpression.class.getName() +
                            "\" does not accept simple sql builder"
            );
        }
        ((BatchSqlBuilder)builder).value(prop);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<V> getType() {
        return (Class<V>) prop.getReturnClass();
    }

    @Override
    public int precedence() {
        return 0;
    }

    static class Str extends OptimisticLockNewValueExpression<String> implements StringExpressionImplementor {

        Str(TypedProp.Scalar<?, String> prop) {
            super(prop);
        }
    }

    static class Num<N extends Number & Comparable<N>> extends OptimisticLockNewValueExpression<N> implements NumericExpressionImplementor<N> {

        Num(TypedProp.Scalar<?, N> prop) {
            super(prop);
        }
    }

    static class Cmp<V extends Comparable<?>> extends OptimisticLockNewValueExpression<V> implements ComparableExpressionImplementor<V> {

        Cmp(TypedProp.Scalar<?, V> prop) {
            super(prop);
        }
    }
}
