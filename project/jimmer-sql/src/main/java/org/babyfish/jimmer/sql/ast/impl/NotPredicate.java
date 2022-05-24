package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class NotPredicate extends AbstractPredicate {

    private Predicate predicate;

    public NotPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast)predicate).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        builder.sql("not ");
        renderChild((Ast) predicate, builder);
    }

    @Override
    public int precedence() {
        return 5;
    }
}
