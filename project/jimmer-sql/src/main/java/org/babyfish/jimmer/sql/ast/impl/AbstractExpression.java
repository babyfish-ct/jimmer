package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;

abstract class AbstractExpression<T> implements Expression<T>, Ast {

    @Override
    public Predicate eq(Expression<T> other) {
        return new ComparisonPredicate.Eq(this, (AbstractExpression<?>)other);
    }

    @Override
    public Predicate ne(Expression<T> other) {
        return new ComparisonPredicate.Ne(this, (AbstractExpression<?>)other);
    }

    @Override
    public Predicate isNull() {
        return new NullityPredicate(this, false);
    }

    @Override
    public Predicate isNotNull() {
        return new NullityPredicate(this, true);
    }

    @Override
    public Predicate in(Collection<T> values) {
        return null;
    }

    @Override
    public Predicate notIn(Collection<T> values) {
        return null;
    }

    @Override
    public Predicate in(TypedSubQuery<T> subQuery) {
        return null;
    }

    @Override
    public Predicate notIn(TypedSubQuery<T> subQuery) {
        return null;
    }

    public abstract void renderTo(SqlBuilder builder);

    protected void renderChild(Ast ast, SqlBuilder builder) {
        if (!(ast instanceof AbstractExpression<?>) ||
                ((AbstractExpression<?>)ast).precedence() <= precedence()) {
            ast.renderTo(builder);
        } else {
            builder.sql("(");
            ast.renderTo(builder);
            builder.sql(")");
        }
    }

    /*
     * Copy from SQL server documentation
     *
     * 1 ~ (Bitwise NOT)
     * 2 * (Multiplication), / (Division), % (Modulus)
     * 3 + (Positive), - (Negative), + (Addition), + (Concatenation), - (Subtraction), & (Bitwise AND), ^ (Bitwise Exclusive OR), | (Bitwise OR)
     * 4 =, >, <, >=, <=, <>, !=, !>, !< (Comparison operators)
     * 5 NOT
     * 6 AND
     * 7 ALL, ANY, BETWEEN, IN, LIKE, OR, SOME
     * 8 = (Assignment)
     *
     * Notes: the brackets for sub queries is always generated
     * "ALL, ANY, SOME" look like function, so I still set their precedent to be 0
     */
    protected abstract int precedence();
}
