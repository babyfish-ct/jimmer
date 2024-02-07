package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigurableRootQueryImpl<T extends Table<?>, R>
        extends AbstractConfigurableTypedQueryImpl
        implements ConfigurableRootQuery<T, R>, TypedRootQueryImplementor<R>, ConfigurableRootQuerySource {

    ConfigurableRootQueryImpl(
            TypedQueryData data,
            MutableRootQueryImpl<T> baseQuery
    ) {
        super(data, baseQuery);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> getBaseQuery() {
        return (MutableRootQueryImpl<T>) super.getBaseQuery();
    }

    @Override
    public <P> @NotNull P fetchPage(int pageIndex, int pageSize, Connection con, PageFactory<R, P> pageFactory) {
        if (pageSize == 0 || pageSize == -1 || pageSize == Integer.MAX_VALUE) {
            List<R> rows = execute(con);
            return pageFactory.create(
                    rows,
                    rows.size(),
                    this
            );
        }
        if (pageIndex < 0) {
            return pageFactory.create(
                    Collections.emptyList(),
                    0,
                    this
            );
        }

        long offset = (long)pageIndex * pageSize;
        if (offset > Long.MAX_VALUE - pageSize) {
            throw new IllegalArgumentException("offset is too big");
        }
        long total = fetchUnlimitedCount(con);
        if (offset >= total) {
            return pageFactory.create(
                    Collections.emptyList(),
                    total,
                    this
            );
        }

        ConfigurableRootQuery<?, R> reversedQuery = null;
        if (offset + pageSize / 2 > total / 2) {
            reversedQuery = reverseSorting();
        }

        List<R> rows;
        if (reversedQuery != null) {
            int limit;
            long reversedOffset = (int)(total - offset - pageSize);
            if (reversedOffset < 0) {
                limit = pageSize + (int)reversedOffset;
                reversedOffset = 0;
            } else {
                limit = pageSize;
            }
            rows = reversedQuery
                    .limit(limit, reversedOffset)
                    .execute(con);
            Collections.reverse(rows);
        } else {
            rows = limit(pageSize, offset).execute(con);
        }
        return pageFactory.create(
                rows,
                total,
                this
        );
    }

    @Override
    public <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    ) {
        if (getData().oldSelections != null) {
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
            for (Selection<?> selection : getData().selections) {
                Ast.from(selection, visitor.getAstContext()).accept(visitor);
            }
        } finally {
            astContext.popStatement();
        }
        ConfigurableRootQuery<T, X> reselected = block.apply(
                baseQuery,
                baseQuery.getTable()
        );
        List<Selection<?>> selections = ((ConfigurableRootQueryImpl<T, X>)reselected).getData().selections;
        return new ConfigurableRootQueryImpl<>(
                getData().reselect(selections),
                baseQuery
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> distinct() {
        TypedQueryData data = getData();
        if (data.distinct) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.distinct(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> limit(int limit) {
        return limitImpl(limit, null);
    }

    @Override
    public ConfigurableRootQuery<T, R> offset(long offset) {
        return limitImpl(null, offset);
    }

    @Override
    public ConfigurableRootQuery<T, R> limit(int limit, long offset) {
        return limitImpl(limit, offset);
    }

    private ConfigurableRootQuery<T, R> limitImpl(@Nullable Integer limit, @Nullable Long offset) {
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
            throw new IllegalArgumentException("'offsetValue' can not be less than 0");
        }
        return new ConfigurableRootQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> withoutSortingAndPaging() {
        TypedQueryData data = getData();
        if (data.withoutSortingAndPaging) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.withoutSortingAndPaging(),
                getBaseQuery()
        );
    }

    @Override
    @Nullable
    public ConfigurableRootQuery<T, R> reverseSorting() {
        TypedQueryData data = this.getData();
        if (data.reverseSorting) {
            return this;
        }
        if (getBaseQuery().getOrders().isEmpty()) {
            return null;
        }
        return new ConfigurableRootQueryImpl<>(
                data.reverseSorting(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> forUpdate(boolean forUpdate) {
        if (!forUpdate) {
            return this;
        }
        TypedQueryData data = getData();
        return new ConfigurableRootQueryImpl<>(
                data.forUpdate(),
                getBaseQuery()
        );
    }

    @Override
    public List<R> execute() {
        return getBaseQuery()
                .getSqlClient()
                .getSlaveConnectionManager(getData().forUpdate)
                .execute(this::executeImpl);
    }

    @Override
    public List<R> execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return getBaseQuery()
                .getSqlClient()
                .getSlaveConnectionManager(getData().forUpdate)
                .execute(this::executeImpl);
    }

    private List<R> executeImpl(Connection con) {
        TypedQueryData data = getData();
        if (data.limit == 0) {
            return Collections.emptyList();
        }
        JSqlClientImplementor sqlClient = getBaseQuery().getSqlClient();
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                data.selections,
                getBaseQuery().getPurpose()
        );
    }

    @Override
    public <X> List<X> map(Connection con, Function<R, X> mapper) {
        List<R> rows = execute(con);
        List<X> mapped = new ArrayList<>(rows.size());
        for (R row : rows) {
            mapped.add(mapper.apply(row));
        }
        return mapped;
    }

    @Override
    public void forEach(Connection con, int batchSize, Consumer<R> consumer) {
        TypedQueryData data = getData();
        if (data.limit == 0) {
            return;
        }
        JSqlClientImplementor sqlClient = getBaseQuery().getSqlClient();
        int finalBatchSize = batchSize > 0 ? batchSize : sqlClient.getDefaultBatchSize();
        if (con != null) {
            forEachImpl(con, finalBatchSize, consumer);
        } else {
            sqlClient.getSlaveConnectionManager(getData().forUpdate).execute(newConn -> {
                forEachImpl(newConn, finalBatchSize, consumer);
                return (Void) null;
            });
        }
    }

    private void forEachImpl(Connection con, int batchSize, Consumer<R> consumer) {
        JSqlClientImplementor sqlClient = getBaseQuery().getSqlClient();
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        Selectors.forEach(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                getData().selections,
                getBaseQuery().getPurpose(),
                batchSize,
                consumer
        );
    }

    private Tuple3<String, List<Object>, List<Integer>> preExecute(SqlBuilder builder) {
        getBaseQuery().applyVirtualPredicates(builder.getAstContext());
        getBaseQuery().applyGlobalFilters(builder.getAstContext(), getBaseQuery().getContext().getFilterLevel(), getData().selections);
        accept(new UseTableVisitor(builder.getAstContext()));
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
        return getData().forUpdate;
    }

    @Override
    public List<Order> getOrders() {
        return getBaseQuery().getOrders();
    }

    @Override
    public int getLimit() {
        return getData().limit;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return getBaseQuery().getSqlClient();
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
