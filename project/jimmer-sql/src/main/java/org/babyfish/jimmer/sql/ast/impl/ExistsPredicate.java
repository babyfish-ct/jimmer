package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.ConfigurableSubQueryImpl;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

public class ExistsPredicate extends AbstractPredicate {

    private final TypedSubQuery<?> subQuery;

    private final boolean negative;

    public static ExistsPredicate of(MutableSubQuery subQuery, boolean negative) {
        return new ExistsPredicate(
                subQuery.select(Constants.number(1)),
                negative
        );
    }

    public static ExistsPredicate of(TypedSubQuery<?> subQuery, boolean negative) {
        if (subQuery instanceof ConfigurableSubQuery<?>) {
            return new ExistsPredicate(
                    ((ConfigurableSubQueryImpl<?>)subQuery).getBaseQuery()
                            .select(Constants.number(1)),
                    negative
            );
        }
        return new ExistsPredicate(subQuery, negative);
    }

    ExistsPredicate(TypedSubQuery<?> subQuery, boolean negative) {
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
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.sql(negative ? "not exists" : "exists");
        renderChild((Ast) subQuery, builder);
    }
}
