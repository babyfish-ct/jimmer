package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NotPredicate extends AbstractPredicate {

    private final Predicate predicate;

    public NotPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) predicate).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.sql("not ");
        renderChild((Ast) predicate, builder);
    }

    @Override
    public int precedence() {
        return ExpressionPrecedences.NOT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotPredicate)) return false;
        NotPredicate that = (NotPredicate) o;
        return predicate.equals(that.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate);
    }

    @Override
    public String toString() {
        return "NotPredicate{" +
                "predicate=" + predicate +
                '}';
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(predicate);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        ctx.pushVirtualPredicateContext(VirtualPredicate.Op.AND);
        try {
            Predicate newPredicate = ctx.resolveVirtualPredicate(predicate);
            if (newPredicate == null) {
                return null;
            }
            return new NotPredicate(newPredicate);
        } finally {
            ctx.popVirtualPredicateContext();
        }
    }
}
