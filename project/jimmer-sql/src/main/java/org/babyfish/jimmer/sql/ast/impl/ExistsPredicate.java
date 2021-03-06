package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableTypedSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class ExistsPredicate extends AbstractPredicate {

    private TypedSubQuery<Integer> subQuery;

    private boolean negative;

    public static ExistsPredicate of(MutableSubQuery subQuery, boolean negative) {
        return new ExistsPredicate(
                subQuery.select(Constants.number(1)),
                negative
        );
    }

    public static ExistsPredicate of(TypedSubQuery<?> subQuery, boolean negative) {
        return new ExistsPredicate(
                ((ConfigurableTypedSubQueryImpl<?>)subQuery).getBaseQuery()
                        .select(Constants.number(1)),
                negative
        );
    }

    ExistsPredicate(TypedSubQuery<Integer> subQuery, boolean negative) {
        this.subQuery = subQuery;
        this.negative = negative;
    }

    @Override
    public Predicate not() {
        return new ExistsPredicate(subQuery, !negative);
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        builder.sql(negative ? "not exists " : "exists ");
        renderChild((Ast) subQuery, builder);
    }
}
