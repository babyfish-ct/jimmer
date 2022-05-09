package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;

import java.util.Arrays;
import java.util.Collection;

public interface ExpressionImplementor<T> extends Expression<T> {

    Class<T> getType();

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
    int precedence();

    @Override
    default Predicate eq(Expression<T> other) {
        return new ComparisonPredicate.Eq(this, other);
    }

    @Override
    default Predicate eq(T other) {
        return eq(Literals.any(other));
    }

    @Override
    default Predicate ne(Expression<T> other) {
        return new ComparisonPredicate.Ne(this, other);
    }

    @Override
    default Predicate ne(T other) {
        return ne(Literals.any(other));
    }

    @Override
    default Predicate isNull() {
        return new NullityPredicate(this, false);
    }

    @Override
    default Predicate isNotNull() {
        return new NullityPredicate(this, true);
    }

    @Override
    default Predicate in(Collection<T> values) {
        return new InCollectionPredicate(this, values, false);
    }

    @Override
    default Predicate in(T ... values) {
        return in(Arrays.asList(values));
    }

    @Override
    default Predicate notIn(Collection<T> values) {
        return new InCollectionPredicate(this, values, true);
    }

    @Override
    default Predicate notIn(T ... values) {
        return notIn(Arrays.asList(values));
    }

    @Override
    default Predicate in(TypedSubQuery<T> subQuery) {
        return new InSubQueryPredicate(this, subQuery, false);
    }

    @Override
    default Predicate notIn(TypedSubQuery<T> subQuery) {
        return new InSubQueryPredicate(this, subQuery, true);
    }

    @Override
    default NumericExpression<Long> count() {
        return count(false);
    }

    @Override
    default NumericExpression<Long> count(boolean distinct) {
        if (distinct) {
            return new AggregationExpression.CountDistinct(this);
        }
        return new AggregationExpression.Count(this);
    }

    @Override
    default Expression<T> coalesce(T defaultValue) {
        return coalesceBuilder().or(defaultValue).build();
    }

    @Override
    default CoalesceBuilder<T> coalesceBuilder() {
        return new CoalesceBuilder<>(this);
    }
}
