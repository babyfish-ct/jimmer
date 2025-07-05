package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

public class MergedTypedSubQueryImpl<R> extends AbstractExpression<R> implements TypedSubQuery<R>, TypedQueryImplementor  {

    private final JSqlClientImplementor sqlClient;

    private final String operator;

    private final TypedQueryImplementor[] queries;

    private final List<Selection<?>> selections;

    private MergedTypedSubQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedSubQuery<R>[] queries
    ) {
        this.sqlClient = sqlClient;
        this.operator = operator;
        TypedQueryImplementor[] arr = new TypedQueryImplementor[queries.length];
        for (int i = 0; i < queries.length; i++) {
            arr[i] = (TypedQueryImplementor) queries[i];
        }
        this.queries = arr;
        selections = mergedSelections(this.queries);
    }

    @SafeVarargs
    public static <R> TypedSubQuery<R> of(
            String operator,
            TypedSubQuery<R> ... queries
    ) {
        if (queries.length == 0) {
            throw new IllegalArgumentException("No queries are specified");
        }
        JSqlClientImplementor sqlClient = ((TypedQueryImplementor)queries[0]).getSqlClient();
        return of(sqlClient, operator, queries);
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R> TypedSubQuery<R> of(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedSubQuery<R>... queries
    ) {
        switch (queries.length) {
            case 0:
                throw new IllegalArgumentException("No queries are specified");
            case 1:
                return queries[0];
            default:
                Class<?> type = ((ExpressionImplementor<?>)queries[0]).getType();
                if (type == String.class) {
                    return (TypedSubQuery<R>) new Str(
                            sqlClient,
                            operator,
                            (TypedSubQuery<String>[]) queries
                    );
                }
                if (Number.class.isAssignableFrom(type)) {
                    return new Num<>(
                            sqlClient,
                            operator,
                            (TypedSubQuery[]) queries
                    );
                }
                if (Date.class.isAssignableFrom(type)) {
                    return new Dt<>(
                            sqlClient,
                            operator,
                            (TypedSubQuery[]) queries
                    );
                }
                if (Temporal.class.isAssignableFrom(type)) {
                    return new Tp<>(
                            sqlClient,
                            operator,
                            (TypedSubQuery[]) queries
                    );
                }
                if (Comparable.class.isAssignableFrom(type)) {
                    return new Cmp<>(
                            sqlClient,
                            operator,
                            (TypedSubQuery[]) queries
                    );
                }
                return new MergedTypedSubQueryImpl<>(
                        sqlClient,
                        operator,
                        queries
                );
        }
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (TypedQueryImplementor query : queries) {
            query.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        boolean addOperator = false;
        for (TypedQueryImplementor query : queries) {
            if (addOperator) {
                builder.space('?').sql(operator).space('?');
            } else {
                addOperator = true;
            }
            query.renderTo(builder);
        }
        builder.leave();
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        for (TypedQueryImplementor query : queries) {
            if (hasVirtualPredicate(query)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        for (TypedQueryImplementor query : queries) {
            ctx.resolveVirtualPredicate(query);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<R> getType() {
        return ((ExpressionImplementor<R>)queries[0]).getType();
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

    private static List<Selection<?>> mergedSelections(TypedQueryImplementor[] queries) {
        List<Selection<?>> selections = queries[0].getSelections();
        int size = selections.size();
        for (int i = 0; i < queries.length; i++) {
            List<Selection<?>> otherSelections = queries[i].getSelections();
            if (size != otherSelections.size()) {
                throw new IllegalArgumentException(
                        "Cannot merged sub queries with different selections"
                );
            }
            for (int index = 0; index < size; index++) {
                if (!isSameType(selections.get(index), otherSelections.get(index))) {
                    throw new IllegalArgumentException(
                            "Cannot merged sub queries with different selections"
                    );
                }
            }
        }
        return selections;
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

    static class Str extends MergedTypedSubQueryImpl<String> implements TypedSubQuery.Str, StringExpressionImplementor {

        Str(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<String>[] queries) {
            super(sqlClient, operator, queries);
        }
    }

    private static class Num<N extends Number & Comparable<N>> extends MergedTypedSubQueryImpl<N> implements TypedSubQuery.Num<N>, NumericExpressionImplementor<N> {

        Num(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<N>[] queries) {
            super(sqlClient, operator, queries);
        }
    }

    private static class Cmp<T extends Comparable<?>> extends MergedTypedSubQueryImpl<T> implements TypedSubQuery.Cmp<T>, ComparableExpressionImplementor<T> {

        Cmp(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<T>[] queries) {
            super(sqlClient, operator, queries);
        }
    }

    private static class Dt<T extends Date> extends MergedTypedSubQueryImpl<T> implements TypedSubQuery.Dt<T>, DateExpressionImplementor<T> {

        Dt(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<T>[] queries) {
            super(sqlClient, operator, queries);
        }
    }

    private static class Tp<T extends Temporal & Comparable<?>> extends MergedTypedSubQueryImpl<T> implements TypedSubQuery.Tp<T>, TemporalExpressionImplementor<T> {

        Tp(JSqlClientImplementor sqlClient, String operator, TypedSubQuery<T>[] queries) {
            super(sqlClient, operator, queries);
        }
    }
}
