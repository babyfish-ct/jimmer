package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExistsPredicate;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.SubQueryFunctionExpression;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

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
        List<Selection<?>> selections = data.getSelections();
        switch (selections.size()) {
            case 1:
                Selection<?> selection = selections.get(0);
                if (selection instanceof Table<?>) {
                    type = (Class<R>) ((Table<?>) selection).getImmutableType().getJavaClass();
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
        baseQuery.freeze();
    }

    @Override
    public Class<R> getType() {
        return type;
    }

    @Override
    public MutableSubQueryImpl getBaseQuery() {
        return (MutableSubQueryImpl) super.getBaseQuery();
    }

    @Override
    public ConfigurableSubQuery<R> limit(int limit, int offset) {
        TypedQueryData data = getData();
        if (data.getLimit() == limit && data.getOffset() == offset) {
            return this;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' can not be less than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' can not be less than 0");
        }
        if (limit > Integer.MAX_VALUE - offset) {
            throw new IllegalArgumentException("'limit' > Int.MAX_VALUE - offset");
        }
        return new ConfigurableSubQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableSubQuery<R> distinct() {
        TypedQueryData data = getData();
        if (data.isDistinct()) {
            return this;
        }
        return new ConfigurableSubQueryImpl<>(
                data.distinct(),
                getBaseQuery()
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
        if (visitor.visitSubQuery(this)) {
            getBaseQuery().setParent(visitor.getAstContext().getStatement());
            super.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        super.renderTo(builder);
        builder.leave();
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public TypedSubQuery<R> union(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(getBaseQuery().getSqlClient(), "union", this, other);
    }

    @Override
    public TypedSubQuery<R> unionAll(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(getBaseQuery().getSqlClient(), "union all", this, other);
    }

    @Override
    public TypedSubQuery<R> minus(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(getBaseQuery().getSqlClient(), "minus", this, other);
    }

    @Override
    public TypedSubQuery<R> intersect(TypedSubQuery<R> other) {
        return new MergedTypedSubQueryImpl<>(getBaseQuery().getSqlClient(), "intersect", this, other);
    }
}