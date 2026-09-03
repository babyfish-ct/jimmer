package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

abstract class AbstractBaseTableExpression<T, E extends ExpressionImplementor<T>>
        implements ExpressionImplementor<T>, Ast {

    private final E raw;

    private final BaseTableOwner baseTableOwner;

    AbstractBaseTableExpression(E raw, BaseTableOwner baseTableOwner) {
        this.raw = raw;
        this.baseTableOwner = baseTableOwner;
    }

    final E raw() {
        return raw;
    }

    final BaseTableOwner getBaseTableOwner() {
        return baseTableOwner;
    }

    protected Ast rawAst() {
        return (Ast) raw;
    }

    @Override
    public final Class<T> getType() {
        return raw.getType();
    }

    @Override
    public final int precedence() {
        return raw.precedence();
    }

    @Override
    public final void accept(@NotNull AstVisitor visitor) {
        AstContext ctx = visitor.getAstContext();
        Object replacement = ctx.getMutationExpressionReplacement(this);
        if (replacement != null) {
            if (replacement instanceof Ast) {
                ((Ast) replacement).accept(visitor);
            }
            return;
        }
        visitor.visitBaseTableExpression(baseTableOwner, this);
        baseTableOwner.visitOwnerStatementChain(ctx, () -> rawAst().accept(visitor));
    }

    @Override
    public final void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        renderWithMutationReplacement(builder, () -> renderWithoutReplacement(builder));
    }

    protected abstract void renderWithoutReplacement(AbstractSqlBuilder<?> builder);

    protected final void renderWithMutationReplacement(
            AbstractSqlBuilder<?> builder,
            Runnable renderWithoutReplacement
    ) {
        Object replacement = builder.getMutationExpressionReplacement(this);
        if (replacement instanceof Ast) {
            ((Ast) replacement).renderTo(builder);
        } else if (replacement != null) {
            builder.sql((String) replacement);
        } else {
            renderWithoutReplacement.run();
        }
    }

    @Override
    public final boolean hasVirtualPredicate() {
        return rawAst().hasVirtualPredicate();
    }

    @Override
    public final Ast resolveVirtualPredicate(AstContext ctx) {
        return rawAst().resolveVirtualPredicate(ctx);
    }
}
