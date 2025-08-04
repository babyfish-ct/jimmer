package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

public class ConfigurableSubQueryImpl<R>
        extends AbstractConfigurableTypedQueryImpl
        implements ConfigurableSubQuery<R>, ExpressionImplementor<R> {

    private final Class<R> type;

    @SuppressWarnings("unchecked")
    ConfigurableSubQueryImpl(
            TypedQueryData data,
            MutableSubQueryImpl baseQuery
    ) {
        super(data, baseQuery);
        List<Selection<?>> selections = data.selections;
        switch (selections.size()) {
            case 1:
                Selection<?> selection = selections.get(0);
                if (selection instanceof TableTypeProvider) {
                    type = (Class<R>) ((TableTypeProvider)selection).getImmutableType().getJavaClass();
                } else {
                    type = (Class<R>)((ExpressionImplementor<?>)selection).getType();
                }
                break;
            case 2:
                type = (Class<R>) Tuple2.class;
                break;
            case 3:
                type = (Class<R>) Tuple3.class;
                break;
            case 4:
                type = (Class<R>) Tuple4.class;
                break;
            case 5:
                type = (Class<R>) Tuple5.class;
                break;
            case 6:
                type = (Class<R>) Tuple6.class;
                break;
            case 7:
                type = (Class<R>) Tuple7.class;
                break;
            case 8:
                type = (Class<R>) Tuple8.class;
                break;
            case 9:
                type = (Class<R>) Tuple9.class;
                break;
            default:
                throw new IllegalArgumentException("selection count must between 1 and 9");
        }
    }

    @SuppressWarnings("unchecked")
    static <R> TypedSubQuery<R> of(
            TypedQueryData data,
            MutableSubQueryImpl baseQuery
    ) {
        if (data.selections.size() == 1) {
            Selection<?> selection = data.selections.get(0);
            if (selection instanceof ExpressionImplementor<?>) {
                Class<?> type = ((ExpressionImplementor<?>) selection).getType();
                if (type == String.class) {
                    return (TypedSubQuery<R>) new Str(data, baseQuery);
                }
                if (Number.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Num<>(data, baseQuery);
                }
                if (Comparable.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Cmp<>(data, baseQuery);
                }
                if (Date.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Dt<>(data, baseQuery);
                }
                if (Temporal.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Tp<>(data, baseQuery);
                }
            }
        }
        return new ConfigurableSubQueryImpl<>(data, baseQuery);
    }

    @Override
    public Class<R> getType() {
        return type;
    }

    @Override
    public MutableSubQueryImpl getMutableQuery() {
        return (MutableSubQueryImpl) super.getMutableQuery();
    }

    @Override
    public ConfigurableSubQuery<R> limit(int limit) {
        return limitImpl(limit, null);
    }

    @Override
    public ConfigurableSubQuery<R> offset(long offset) {
        return limitImpl(null, offset);
    }

    @Override
    public ConfigurableSubQuery<R> limit(int limit, long offset) {
        return limitImpl(limit, offset);
    }

    private ConfigurableSubQuery<R> limitImpl(@Nullable Integer limit, @Nullable Long offset) {
        TypedQueryData data = getData();
        if (limit == null) {
            limit = data.limit;
        }
        if (offset == null) {
            offset = data.offset;
        }
        if (data.limit == limit && data.offset == offset) {
            return this;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' can not be less than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' can not be less than 0");
        }
        return new ConfigurableSubQueryImpl<>(
                data.limit(limit, offset),
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableSubQuery<R> distinct() {
        TypedQueryData data = getData();
        if (data.distinct) {
            return this;
        }
        return new ConfigurableSubQueryImpl<>(
                data.distinct(),
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableSubQuery<R> hint(@Nullable String hint) {
        TypedQueryData data = getData();
        return new ConfigurableSubQueryImpl<>(
                data.hint(hint),
                getMutableQuery()
        );
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
    public void accept(@NotNull AstVisitor visitor) {
        getMutableQuery().setParent(visitor.getAstContext().getStatement());
        if (visitor.visitSubQuery(this)) {
            super.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        super.renderTo(builder);
        builder.leave();
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public boolean hasVirtualPredicate() {
        return getMutableQuery().hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        getMutableQuery().resolveVirtualPredicate(ctx);
        return this;
    }

    private static class Str extends ConfigurableSubQueryImpl<String> implements ConfigurableSubQuery.Str, StringExpressionImplementor {

        Str(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }
    }

    private static class Num<N extends Number & Comparable<N>> extends ConfigurableSubQueryImpl<N> implements ConfigurableSubQuery.Num<N>, NumericExpressionImplementor<N> {

        Num(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }
    }

    private static class Cmp<T extends Comparable<?>> extends ConfigurableSubQueryImpl<T> implements ConfigurableSubQuery.Cmp<T>, ComparableExpressionImplementor<T> {

        Cmp(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }
    }

    private static class Dt<T extends Date> extends ConfigurableSubQueryImpl<T> implements ConfigurableSubQuery.Dt<T>, DateExpressionImplementor<T> {

        Dt(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }
    }

    private static class Tp<T extends Temporal & Comparable<?>> extends ConfigurableSubQueryImpl<T> implements ConfigurableSubQuery.Tp<T>, TemporalExpressionImplementor<T> {

        Tp(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }
    }
}