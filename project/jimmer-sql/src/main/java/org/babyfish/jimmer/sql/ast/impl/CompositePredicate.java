package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.associated.VirtualPredicate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public abstract class CompositePredicate extends AbstractPredicate {

    private static final Predicate[] EMPTY_PREDICATE_ARR = new Predicate[0];

    private final Predicate[] predicates;

    CompositePredicate(Predicate[] predicates) {
        this.predicates = predicates;
    }

    public static Predicate and(Predicate ... predicates) {
        return composite(predicates, And::new);
    }

    public static Predicate or(Predicate ... predicates) {
        return composite(predicates, Or::new);
    }

    private static Predicate composite(Predicate[] predicates, BiFunction<Boolean, Predicate[], Predicate> creator) {
        boolean hasVirtualPredicate = false;
        Predicate[] arr = predicates;
        for (Predicate p : predicates) {
            if (p == null) {
                List<Predicate> list = new ArrayList<>(predicates.length - 1);
                for (Predicate p2 : predicates) {
                    if (p2 != null) {
                        if (((Ast)p2).hasVirtualPredicate()) {
                            hasVirtualPredicate = true;
                        }
                        list.add(p2);
                    }
                }
                arr = list.toArray(EMPTY_PREDICATE_ARR);
                break;
            } else if (((Ast)p).hasVirtualPredicate()) {
                hasVirtualPredicate = true;
            }
        }
        if (arr.length == 0) {
            return null;
        }
        if (arr.length == 1) {
            return arr[0];
        }
        return creator.apply(hasVirtualPredicate, arr);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (Predicate predicate : predicates) {
            ((Ast) predicate).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(scopeType());
        for (Predicate predicate : predicates) {
            builder.separator();
            renderChild((Ast) predicate, builder);
        }
        builder.leave();
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(predicates);
    }

    Ast onResolveVirtualPredicate(AstContext ctx, VirtualPredicate.Op op, BiFunction<Boolean, Predicate[], Ast> creator) {
        ctx.pushVirtualPredicateContext(op);
        try {
            return creator.apply(false, ctx.resolveVirtualPredicates(predicates));
        } finally {
            ctx.popVirtualPredicateContext();
        }
    }

    protected abstract SqlBuilder.ScopeType scopeType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        CompositePredicate that = (CompositePredicate) o;
        return Arrays.equals(predicates, that.predicates);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(predicates);
    }

    static class And extends CompositePredicate {

        And(boolean hasVirtualPredicate, Predicate[] predicates) {
            super(predicates);
        }

        @Override
        protected SqlBuilder.ScopeType scopeType() {
            return SqlBuilder.ScopeType.AND;
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.AND;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            return onResolveVirtualPredicate(ctx, VirtualPredicate.Op.AND, And::new);
        }
    }

    static class Or extends CompositePredicate {

        Or(boolean hasVirtualPredicate, Predicate[] predicates) {
            super(predicates);
        }

        @Override
        protected SqlBuilder.ScopeType scopeType() {
            return SqlBuilder.ScopeType.OR;
        }

        @Override
        public int precedence() {
            return ExpressionPrecedences.OR;
        }

        @Override
        public Ast onResolveVirtualPredicate(AstContext ctx) {
            return onResolveVirtualPredicate(ctx, VirtualPredicate.Op.OR, Or::new);
        }
    }
}
