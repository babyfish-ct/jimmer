package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public abstract class CompositePredicate extends AbstractPredicate {

    private final Predicate[] predicates;

    CompositePredicate(Predicate[] predicates) {
        this.predicates = predicates;
    }

    public static Predicate and(Predicate ... predicates) {
        Predicate[] arr = Arrays
                .stream(predicates)
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new
                );
        if (arr.length == 0) {
            return null;
        }
        return new And(arr);
    }

    public static Predicate or(Predicate ... predicates) {
        Predicate[] arr = Arrays
                .stream(predicates)
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new
                );
        if (arr.length == 0) {
            return null;
        }
        return new Or(arr);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (Predicate predicate : predicates) {
            ((Ast) predicate).accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.enter(scopeType());
        for (Predicate predicate : predicates) {
            builder.separator();
            renderChild((Ast) predicate, builder);
        }
        builder.leave();
    }

    protected abstract SqlBuilder.ScopeType scopeType();

    static class And extends CompositePredicate {

        And(Predicate ... predicates) {
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
    }

    static class Or extends CompositePredicate {

        Or(Predicate ... predicates) {
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
    }
}
