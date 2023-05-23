package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class NotPredicate extends AbstractPredicate {

    private Predicate predicate;

    public NotPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast)predicate).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
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
}
