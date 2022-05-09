package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class LikePredicate extends AbstractPredicate {

    private StringExpression expression;

    private String pattern;

    private boolean insensitive;

    private boolean negative;

    public static LikePredicate of(
            StringExpression expression,
            String pattern,
            boolean insensitive,
            LikeMode likeMode
    ) {
        if (!likeMode.isStartExact() && !pattern.startsWith("%")) {
            pattern = '%' + pattern;
        }
        if (!likeMode.isEndExact() && !pattern.endsWith("%")) {
            pattern = pattern + '%';
        }
        if (insensitive) {
            pattern = pattern.toLowerCase();
        }
        return new LikePredicate(expression, pattern, insensitive, false);
    }

    private LikePredicate(
            StringExpression expression,
            String pattern,
            boolean insensitive,
            boolean negative
    ) {
        this.expression = expression;
        this.pattern = pattern;
        this.insensitive = insensitive;
        this.negative = negative;
    }

    @Override
    public int precedence() {
        return 7;
    }

    @Override
    public Predicate not() {
        return new LikePredicate(
                expression,
                pattern,
                insensitive,
                !negative
        );
    }

    @Override
    public void accept(AstVisitor visitor) {
        ((Ast)expression).accept(visitor);
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        if (insensitive) {
            builder.sql("lower(");
            renderChild((Ast) expression, builder);
            builder.sql(")");
        } else {
            renderChild((Ast) expression, builder);
        }
        builder.sql(negative ? " not like " : " like ");
        builder.variable(pattern);
    }
}
