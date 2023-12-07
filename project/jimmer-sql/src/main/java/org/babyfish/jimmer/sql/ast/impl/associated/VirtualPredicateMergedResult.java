package org.babyfish.jimmer.sql.ast.impl.associated;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VirtualPredicateMergedResult extends AbstractPredicate {

    private final AbstractMutableStatementImpl parent;

    private final VirtualPredicate.Op op;

    private final List<VirtualPredicate> virtualPredicates = new ArrayList<>();

    private Predicate finalPredicate;

    public VirtualPredicateMergedResult(AbstractMutableStatementImpl parent, VirtualPredicate.Op op) {
        this.parent = parent;
        this.op = op;
    }

    public void merge(VirtualPredicate predicate) {
        if (predicate != null) {
            virtualPredicates.add(predicate);
        }
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)predicate()).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        ((Ast)predicate()).renderTo(builder);
    }

    @Override
    public int precedence() {
        return ((ExpressionImplementor<?>)predicate()).precedence();
    }

    private Predicate predicate() {
        Predicate predicate = finalPredicate;
        if (predicate == null) {
            finalPredicate = predicate = virtualPredicates.get(0).toFinalPredicate(parent, virtualPredicates, op);
        }
        return predicate;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(predicate());
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return (Ast) ctx.resolveVirtualPredicate(predicate());
    }
}
