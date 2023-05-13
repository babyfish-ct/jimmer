package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergedTypedSubQueryImpl<R> implements ExpressionImplementor<R>, TypedSubQuery<R>, TypedQueryImplementor  {

    private final JSqlClientImplementor sqlClient;

    private final String operator;

    private final TypedQueryImplementor left;

    private final TypedQueryImplementor right;

    private final List<Selection<?>> selections;

    public MergedTypedSubQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedSubQuery<R> left,
            TypedSubQuery<R> right) {
        this.sqlClient = sqlClient;
        this.operator = operator;
        this.left = (TypedQueryImplementor) left;
        this.right = (TypedQueryImplementor) right;
        selections = mergedSelections(
                this.left.getSelections(),
                this.right.getSelections()
        );
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        left.accept(visitor);
        right.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        left.renderTo(builder);
        builder.space('?').sql(operator).space('?');
        right.renderTo(builder);
        builder.leave();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<R> getType() {
        return ((ExpressionImplementor<R>)left).getType();
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public Expression<R> all() {
        return new SubQueryFunctionExpression.All<>(this);
    }

    @Override
    public Expression<R> any() {
        return new SubQueryFunctionExpression.Any<>(this);
    }

    @Override
    public Predicate exists() {
        return ExistsPredicate.of(this, false);
    }

    @Override
    public Predicate notExists() {
        return ExistsPredicate.of(this, true);
    }

    @Override
    public TypedSubQuery<R> union(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(sqlClient, "union", this, other);
    }

    @Override
    public TypedSubQuery<R> unionAll(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(sqlClient, "union all", this, other);
    }

    @Override
    public TypedSubQuery<R> minus(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(sqlClient, "minus", this, other);
    }

    @Override
    public TypedSubQuery<R> intersect(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(sqlClient, "intersect", this, other);
    }

    private static List<Selection<?>> mergedSelections(
            List<Selection<?>> list1,
            List<Selection<?>> list2
    ) {
        if (list1.size() != list2.size()) {
            throw new IllegalArgumentException(
                    "Cannot merged sub queries with different selections"
            );
        }
        int size = list1.size();
        for (int index = 0; index < size; index++) {
            if (!isSameType(list1.get(index), list2.get(index))) {
                throw new IllegalArgumentException(
                        "Cannot merged sub queries with different selections"
                );
            }
        }
        return list1;
    }

    private static boolean isSameType(Selection<?> a, Selection<?> b) {
        if (a instanceof Table<?> && b instanceof Table<?>) {
            return ((Table<?>) a).getImmutableType() == ((Table<?>) b).getImmutableType();
        }
        if (a instanceof Expression<?> && b instanceof Expression<?>) {
            return ((ExpressionImplementor<?>) a).getType() ==
                    ((ExpressionImplementor<?>) b).getType();
        }
        return false;
    }
}
