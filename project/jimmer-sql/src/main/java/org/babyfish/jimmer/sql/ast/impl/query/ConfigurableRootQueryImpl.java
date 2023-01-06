package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ConfigurableRootQueryImpl<T extends Table<?>, R>
        extends AbstractConfigurableTypedQueryImpl
        implements ConfigurableRootQuery<T, R>, ConfigurableRootQueryImplementor<T, R> {

    public ConfigurableRootQueryImpl(
            TypedQueryData data,
            MutableRootQueryImpl<T> baseQuery
    ) {
        super(data, baseQuery);
        baseQuery.freeze();
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> getBaseQuery() {
        return (MutableRootQueryImpl<T>) super.getBaseQuery();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    ) {
        if (getData().getOldSelections() != null) {
            throw new IllegalStateException("The current query has been reselected, it cannot be reselect again");
        }
        MutableRootQueryImpl<T> baseQuery = getBaseQuery();
        if (baseQuery.isGroupByClauseUsed()) {
            throw new IllegalStateException("The current query uses group by clause, it cannot be reselected");
        }

        AstContext astContext = new AstContext(baseQuery.getSqlClient());
        AstVisitor visitor = new ReselectValidator(astContext);
        astContext.pushStatement(baseQuery);
        try {
            for (Selection<?> selection : getData().getSelections()) {
                Ast.from(selection, visitor.getAstContext()).accept(visitor);
            }
        } finally {
            astContext.popStatement();
        }
        ConfigurableRootQuery<T, X> reselected = block.apply(
                baseQuery,
                baseQuery.getTable()
        );
        List<Selection<?>> selections = ((ConfigurableRootQueryImpl<T, X>)reselected).getData().getSelections();
        return new ConfigurableRootQueryImpl<>(
                getData().reselect(selections),
                baseQuery
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> distinct() {
        TypedQueryData data = getData();
        if (data.isDistinct()) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.distinct(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> limit(int limit, int offset) {
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
        return new ConfigurableRootQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> withoutSortingAndPaging() {
        TypedQueryData data = getData();
        if (data.isWithoutSortingAndPaging()) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.withoutSortingAndPaging(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> forUpdate() {
        TypedQueryData data = getData();
        if (data.isForUpdate()) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.forUpdate(),
                getBaseQuery()
        );
    }

    @Override
    public List<R> execute() {
        return getBaseQuery()
                .getSqlClient()
                .getSlaveConnectionManager(getData().isForUpdate())
                .execute(this::executeImpl);
    }

    @Override
    public List<R> execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return getBaseQuery()
                .getSqlClient()
                .getSlaveConnectionManager(getData().isForUpdate())
                .execute(this::executeImpl);
    }

    private List<R> executeImpl(Connection con) {
        TypedQueryData data = getData();
        if (data.getLimit() == 0) {
            return Collections.emptyList();
        }
        JSqlClient sqlClient = getBaseQuery().getSqlClient();
        Tuple2<String, List<Object>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                data.getSelections(),
                getBaseQuery().getPurpose()
        );
    }

    @Override
    public void forEach(Connection con, int batchSize, Consumer<R> consumer) {
        TypedQueryData data = getData();
        if (data.getLimit() == 0) {
            return;
        }
        JSqlClient sqlClient = getBaseQuery().getSqlClient();
        int finalBatchSize = batchSize > 0 ? batchSize : sqlClient.getDefaultBatchSize();
        if (con != null) {
            forEachImpl(con, finalBatchSize, consumer);
        } else {
            sqlClient.getSlaveConnectionManager(getData().isForUpdate()).execute(newConn -> {
                forEachImpl(newConn, finalBatchSize, consumer);
                return (Void) null;
            });
        }
    }

    private void forEachImpl(Connection con, int batchSize, Consumer<R> consumer) {
        JSqlClient sqlClient = getBaseQuery().getSqlClient();
        Tuple2<String, List<Object>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        Selectors.forEach(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                getData().getSelections(),
                getBaseQuery().getPurpose(),
                batchSize,
                consumer
        );
    }

    private Tuple2<String, List<Object>> preExecute(SqlBuilder builder) {
        AstVisitor visitor = new UseTableVisitor(builder.getAstContext());
        accept(visitor);
        renderTo(builder);
        return builder.build();
    }

    @Override
    public TypedRootQuery<R> union(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(getBaseQuery().getSqlClient(), "union", this, other);
    }

    @Override
    public TypedRootQuery<R> unionAll(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(getBaseQuery().getSqlClient(), "union all", this, other);
    }

    @Override
    public TypedRootQuery<R> minus(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(getBaseQuery().getSqlClient(), "minus", this, other);
    }

    @Override
    public TypedRootQuery<R> intersect(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(getBaseQuery().getSqlClient(), "intersect", this, other);
    }

    @Override
    public boolean isForUpdate() {
        return getData().isForUpdate();
    }

    @Override
    public List<Order> getOrders() {
        return getBaseQuery().getOrders();
    }

    private static class ReselectValidator extends AstVisitor {

        ReselectValidator(AstContext astContext) {
            super(astContext);
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
