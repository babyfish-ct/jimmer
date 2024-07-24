package org.babyfish.jimmer.sql.ast.impl.associated;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VirtualPredicateMergedResult extends AbstractPredicate {

    private static final Predicate NIL_PREDICATE = new AbstractPredicate() {

        @Override
        protected boolean determineHasVirtualPredicate() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int precedence() {
            throw new UnsupportedOperationException();
        }
    };

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
        Ast ast = (Ast) predicate();
        if (ast != null) {
            ast.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        Ast ast = (Ast) predicate();
        if (ast != null) {
            ast.renderTo(builder);
        } else {
            builder.sql("1 = 1");
        }
    }

    @Override
    public int precedence() {
        return ((ExpressionImplementor<?>)predicate()).precedence();
    }

    private Predicate predicate() {
        Predicate predicate = finalPredicate;
        if (predicate == null) {
            predicate = virtualPredicates.get(0).toFinalPredicate(parent, virtualPredicates, op);
            if (predicate == null) {
                predicate = NIL_PREDICATE;
            }
            finalPredicate = predicate;
        }
        return predicate == NIL_PREDICATE ? null : predicate;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(predicate());
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return (Ast) ctx.resolveVirtualPredicate(predicate());
    }

    public static void removeEmptyResult(List<?> expressions) {
        Iterator<?> itr = expressions.iterator();
        while (itr.hasNext()) {
            Object expression = itr.next();
            if (expression instanceof PredicateWrapper) {
                PredicateWrapper wrapper = (PredicateWrapper) expression;
                expression = wrapper.unwrap();
            }
            if (expression instanceof VirtualPredicateMergedResult &&
                    ((VirtualPredicateMergedResult)expression).predicate() == null) {
                itr.remove();
            }
        }
    }
}
