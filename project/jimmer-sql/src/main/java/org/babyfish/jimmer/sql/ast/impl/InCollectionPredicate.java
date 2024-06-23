package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class InCollectionPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private final Collection<?> values;

    private final boolean nullable;

    private final boolean negative;

    public InCollectionPredicate(
            Expression<?> expression,
            Collection<?> values,
            boolean nullable,
            boolean negative
    ) {
        this.expression = expression;
        this.values = values;
        this.nullable = nullable;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        Ast.of(expression).accept(visitor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        Map<List<ValueGetter>, List<Object>> multiMap = new LinkedHashMap<>();
        for (Object value : values) {
            multiMap.computeIfAbsent(
                    ValueGetter.valueGetters(builder.sqlClient(), (Expression<Object>) expression, value),
                    it -> new ArrayList<>()
            ).add(value);
        }
        if (multiMap.size() == 1) {
            Map.Entry<List<ValueGetter>, List<Object>> e = multiMap.entrySet().iterator().next();
            ComparisonPredicates.renderIn(
                    nullable,
                    negative,
                    e.getKey(),
                    e.getValue(),
                    builder
            );
            return;
        }
        builder.enter(negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.SMART_OR);
        for (Map.Entry<List<ValueGetter>, List<Object>> e : multiMap.entrySet()) {
            builder.separator();
            ComparisonPredicates.renderIn(
                    nullable,
                    negative,
                    e.getKey(),
                    e.getValue(),
                    builder
            );
        }
        builder.leave();
        //ComparisonPredicates.renderInCollection(nullable, negative, (ExpressionImplementor<?>) expression, values, builder);
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(expression);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        this.expression = ctx.resolveVirtualPredicate(expression);
        return this;
    }

    @Override
    public Predicate not() {
        return new InCollectionPredicate(expression, values, nullable, !negative);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InCollectionPredicate)) return false;
        InCollectionPredicate that = (InCollectionPredicate) o;
        return negative == that.negative && expression.equals(that.expression) && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, values, negative);
    }
}
