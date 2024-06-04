package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.impl.util.BatchSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class OptimisticLockNewValueExpression<N extends Number & Comparable<N>>
        extends AbstractExpression<N>
        implements NumericExpressionImplementor<N> {

    private final ImmutableProp prop;

    OptimisticLockNewValueExpression(TypedProp.Scalar<?, N> prop) {
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
    public void renderTo(@NotNull SqlBuilder builder) {
        throw new IllegalStateException(
                "The user optimistic lock literal does not accept simple sql builder"
        );
    }

    @Override
    public void renderTo(@NotNull BatchSqlBuilder builder) {
        builder.value(prop);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<N> getType() {
        return (Class<N>) prop.getReturnClass();
    }

    @Override
    public int precedence() {
        return 0;
    }
}
