package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class ConfigurableTypedRootQueryImpl<R>
        extends AbstractConfigurableTypedQueryImpl<R>
        implements ConfigurableTypedRootQuery<R> {

    public ConfigurableTypedRootQueryImpl(
            TypedQueryData data,
            RootMutableQueryImpl baseQuery
    ) {
        super(data, baseQuery);
    }

    @Override
    public RootMutableQueryImpl getBaseQuery() {
        return (RootMutableQueryImpl) super.getBaseQuery();
    }

    @Override
    public <X> ConfigurableTypedRootQuery<X> reselect(Function<RootSelectable, ConfigurableTypedRootQuery<X>> block) {
        if (getData().getOldSelections() != null) {
            throw new IllegalStateException("The current query has been reselected, it cannot be reselect again");
        }
        if (getBaseQuery().isGroupByClauseUsed()) {
            throw new IllegalStateException("The current query uses group by clause, it cannot be reselected");
        }
        AstVisitor visitor = new ReselectValidator();
        for (Selection<?> selection : getData().getSelections()) {
            ((Ast)selection).accept(visitor);
        }
        ConfigurableTypedRootQuery<X> reselected = block.apply(getBaseQuery());
        List<Selection<?>> selections = ((ConfigurableTypedRootQueryImpl<X>)reselected).getData().getSelections();
        return new ConfigurableTypedRootQueryImpl<X>(
                getData().reselect(selections),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableTypedRootQuery<R> distinct() {
        TypedQueryData data = getData();
        if (data.isDistinct()) {
            return this;
        }
        return new ConfigurableTypedRootQueryImpl<>(
                data.distinct(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableTypedRootQuery<R> limit(int limit, int offset) {
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
        return new ConfigurableTypedRootQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableTypedRootQuery<R> withoutSortingAndPaging() {
        TypedQueryData data = getData();
        if (data.isWithoutSortingAndPaging()) {
            return this;
        }
        return new ConfigurableTypedRootQueryImpl<>(
                data.withoutSortingAndPaging(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableTypedRootQuery<R> forUpdate() {
        TypedQueryData data = getData();
        if (data.isForUpdate()) {
            return this;
        }
        return new ConfigurableTypedRootQueryImpl<>(
                data.forUpdate(),
                getBaseQuery()
        );
    }

    @Override
    public List<R> execute(Connection con) {
        TypedQueryData data = getData();
        if (getData().getLimit() == 0) {
            return Collections.emptyList();
        }
        SqlClient sqlClient = getBaseQuery().getSqlClient();
        Tuple2<String, List<Object>> sqlResult = preExecute(new SqlBuilder(sqlClient));
        return Selectors.select(
                sqlClient,
                con,
                sqlResult._1(),
                sqlResult._2(),
                data.getSelections()
        );
    }

    private Tuple2<String, List<Object>> preExecute(SqlBuilder builder) {
        AstVisitor visitor = new UseTableVisitor(builder);
        accept(visitor);
        renderTo(builder);
        return builder.build();
    }

    @Override
    public TypedRootQuery<R> union(TypedRootQuery<R> other) {
        return null;
    }

    @Override
    public TypedRootQuery<R> unionAll(TypedRootQuery<R> other) {
        return null;
    }

    @Override
    public TypedRootQuery<R> minus(TypedRootQuery<R> other) {
        return null;
    }

    @Override
    public TypedRootQuery<R> intersect(TypedRootQuery<R> other) {
        return null;
    }

    private static class ReselectValidator extends AstVisitor {

        ReselectValidator() {
            super(null);
        }

        @Override
        public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
            return false;
        }

        @Override
        public void visitAggregation(String functionName, Expression<?> expression, String prefix) {
            throw new IllegalStateException(
                    "The current query uses aggregation function in select clause, it cannot be reselected"
            );
        }
    }
}
