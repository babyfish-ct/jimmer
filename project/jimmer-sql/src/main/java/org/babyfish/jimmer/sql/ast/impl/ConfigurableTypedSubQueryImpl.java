package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.List;

class ConfigurableTypedSubQueryImpl<R>
extends AbstractConfigurableTypedQueryImpl<R>
implements ConfigurableTypedSubQuery<R>, ExpressionImplementor<R> {

    private Class<R> type;

    @SuppressWarnings("unchecked")
    public ConfigurableTypedSubQueryImpl(
            TypedQueryData data,
            SubMutableQueryImpl baseQuery
    ) {
        super(data, baseQuery);
        List<Selection<?>> selections = data.getSelections();
        switch (selections.size()) {
            case 1:
                Selection<?> selection = selections.get(0);
                if (selection instanceof Table<?>) {
                    type = (Class<R>)TableImplementor.unwrap((Table<?>) selection).getImmutableType().getJavaClass();
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

    @Override
    public Class<R> getType() {
        return type;
    }

    @Override
    public SubMutableQueryImpl getBaseQuery() {
        return (SubMutableQueryImpl) super.getBaseQuery();
    }

    @Override
    public ConfigurableTypedSubQuery<R> limit(int limit, int offset) {
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
        return new ConfigurableTypedSubQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableTypedSubQuery<R> distinct() {
        TypedQueryData data = getData();
        if (data.isDistinct()) {
            return this;
        }
        return new ConfigurableTypedSubQueryImpl<>(
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
    public void accept(AstVisitor visitor) {
        if (visitor.visitSubQuery(this)) {
            super.accept(visitor);
        }
    }

    @Override
    public void renderTo(SqlBuilder builder) {
        builder.sql("(");
        super.renderTo(builder);
        builder.sql(")");
    }

    @Override
    public int precedence() {
        return 0;
    }
}
