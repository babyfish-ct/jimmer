package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergedTypedSubQueryImpl<R> extends AbstractExpression<R> implements TypedSubQuery<R>, TypedQueryImplementor  {

    private final JSqlClientImplementor sqlClient;

    private final String operator;

    private final TypedQueryImplementor left;

    private final TypedQueryImplementor right;

    private final List<Selection<?>> selections;

    private MergedTypedSubQueryImpl(
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

    public static <R> TypedSubQuery<R> of(
            String operator,
            TypedSubQuery<R> left,
            TypedSubQuery<R> right
    ) {
        JSqlClientImplementor sqlClient = ((TypedQueryImplementor)left).getSqlClient();
        return of(sqlClient, operator, left, right);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R> TypedSubQuery<R> of(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedSubQuery<R> left,
            TypedSubQuery<R> right
    ) {
        Class<?> leftType = ((ExpressionImplementor<?>)left).getType();
        if (leftType == String.class) {
            return (TypedSubQuery<R>) new AbstractStr.Impl(
                    sqlClient,
                    operator,
                    (TypedSubQuery<String>) left,
                    (TypedSubQuery<String>) right
            );
        }
        if (Number.class.isAssignableFrom(leftType)) {
            return new AbstractNum.Impl<>(
                    sqlClient,
                    operator,
                    (TypedSubQuery) left,
                    (TypedSubQuery) right
            );
        }
        if (Comparable.class.isAssignableFrom(leftType)) {
            return new AbstractCmp.Impl<>(
                    sqlClient,
                    operator,
                    (TypedSubQuery) left,
                    (TypedSubQuery) right
            );
        }
        return new MergedTypedSubQueryImpl<>(
                sqlClient,
                operator,
                left,
                right
        );
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        left.accept(visitor);
        right.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        left.renderTo(builder);
        builder.space('?').sql(operator).space('?');
        right.renderTo(builder);
        builder.leave();
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return hasVirtualPredicate(left) || hasVirtualPredicate(right);
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        ctx.resolveVirtualPredicate(left);
        ctx.resolveVirtualPredicate(right);
        return this;
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
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
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

    static abstract class AbstractStr extends MergedTypedSubQueryImpl<String> implements TypedSubQuery.Str, StringExpressionImplementor {

        AbstractStr(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<String> left, TypedSubQuery<String> right) {
            super(sqlClient, operator, left, right);
        }

        @Override
        public TypedSubQuery.Str union(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.union(other);
        }

        @Override
        public TypedSubQuery.Str unionAll(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Str minus(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.minus(other);
        }

        @Override
        public TypedSubQuery.Str intersect(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.intersect(other);
        }

        static class Impl extends AbstractStr implements StringExpressionImplementor {

            Impl(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<String> left, TypedSubQuery<String> right) {
                super(sqlClient, operator, left, right);
            }
        }
    }

    private static abstract class AbstractNum<N extends Number & Comparable<N>> extends MergedTypedSubQueryImpl<N> implements TypedSubQuery.Num<N>, NumericExpressionImplementor<N> {

        AbstractNum(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<N> left, TypedSubQuery<N> right) {
            super(sqlClient, operator, left, right);
        }

        @Override
        public TypedSubQuery.Num<N> union(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.union(other);
        }

        @Override
        public TypedSubQuery.Num<N> unionAll(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Num<N> minus(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.minus(other);
        }

        @Override
        public TypedSubQuery.Num<N> intersect(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.intersect(other);
        }

        static class Impl<N extends Number & Comparable<N>> extends AbstractNum<N> implements NumericExpressionImplementor<N> {

            Impl(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<N> left, TypedSubQuery<N> right) {
                super(sqlClient, operator, left, right);
            }
        }
    }

    private static abstract class AbstractCmp<T extends Comparable<?>> extends MergedTypedSubQueryImpl<T> implements TypedSubQuery.Cmp<T>, ComparableExpressionImplementor<T> {

        AbstractCmp(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<T> left, TypedSubQuery<T> right) {
            super(sqlClient, operator, left, right);
        }

        @Override
        public TypedSubQuery.Cmp<T> union(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.union(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> unionAll(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> minus(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.minus(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> intersect(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.intersect(other);
        }

        static class Impl<T extends Comparable<?>> extends AbstractCmp<T> implements ComparableExpressionImplementor<T> {

            Impl(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<T> left, TypedSubQuery<T> right) {
                super(sqlClient, operator, left, right);
            }
        }
    }
}
