package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.Slice;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigurableRootQueryImpl<T extends TableLike<?>, R>
        extends AbstractConfigurableTypedQueryImpl
        implements ConfigurableRootQuery<T, R>, TypedRootQueryImplementor<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableRootQueryImpl.class);

    ConfigurableRootQueryImpl(
            TypedQueryData data,
            MutableRootQueryImpl<T> baseQuery
    ) {
        super(data, baseQuery);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MutableRootQueryImpl<T> getMutableQuery() {
        return (MutableRootQueryImpl<T>) super.getMutableQuery();
    }

    @Override
    public <P> @NotNull P fetchPage(int pageIndex, int pageSize, Connection con, PageFactory<R, P> pageFactory) {
        if (pageSize == 0 || pageSize == -1 || pageSize == Integer.MAX_VALUE) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "For meaningless pageSize {}, avoid pagination and fetch all rows directly",
                        pageSize
                );
            }
            List<R> rows = execute(con);
            return pageFactory.create(
                    rows,
                    rows.size(),
                    PageSource.of(0, Integer.MAX_VALUE, getMutableQuery())
            );
        }
        if (pageIndex < 0) {
            LOGGER.info("pageIndex is negative, returns empty list directly");
            return pageFactory.create(
                    Collections.emptyList(),
                    0,
                    PageSource.of(0, pageSize, getMutableQuery())
            );
        }

        long offset = (long) pageIndex * pageSize;
        if (offset > Long.MAX_VALUE - pageSize) {
            throw new IllegalArgumentException("offset is too big");
        }
        long total = fetchUnlimitedCount(con);
        if (offset >= total) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "pageIndex(starts from 0) is {} but the total page count is {}, " +
                                "returns empty list directly",
                        pageIndex,
                        (total + pageSize - 1) / pageSize
                );
            }
            return pageFactory.create(
                    Collections.emptyList(),
                    total,
                    PageSource.of(pageIndex, pageSize, getMutableQuery())
            );
        }

        ConfigurableRootQuery<?, R> reversedQuery = null;
        boolean reverseSortOptimization;
        if (getData().reverseSortOptimizationEnabled != null) {
            reverseSortOptimization = getData().reverseSortOptimizationEnabled;
        } else {
            reverseSortOptimization = getSqlClient().isReverseSortOptimizationEnabled();
        }
        if (reverseSortOptimization && offset + pageSize / 2 > total / 2) {
            LOGGER.info("Enable reverse sorting optimization, all sorting behaviors will be reversed");
            reversedQuery = reverseSorting();
        }

        List<R> rows;
        if (reversedQuery != null) {
            int limit;
            long reversedOffset = (int) (total - offset - pageSize);
            if (reversedOffset < 0) {
                limit = pageSize + (int) reversedOffset;
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
                PageSource.of(pageIndex, pageSize, getMutableQuery())
        );
    }

    @Override
    public Slice<R> fetchSlice(int limit, int offset, @Nullable Connection con) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit cannot be less than 1");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be less than 0");
        }
        List<R> rows = limit(limit + 1).offset(offset).execute(con);
        if (rows.size() <= limit) {
            return new Slice<>(rows, offset == 0, true);
        }
        return new Slice<>(rows.subList(0, rows.size() - 1), offset == 0, false);
    }

    @Override
    public <X> ConfigurableRootQuery<T, X> reselect(
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, X>> block
    ) {
        if (getData().oldSelections != null) {
            throw new IllegalStateException("The current query has been reselected, it cannot be reselect again");
        }
        MutableRootQueryImpl<T> baseQuery = getMutableQuery();
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
        List<Selection<?>> selections = ((ConfigurableRootQueryImpl<T, X>) reselected).getData().selections;
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
                getMutableQuery()
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
                getMutableQuery()
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
                getMutableQuery()
        );
    }

    @Override
    @Nullable
    public ConfigurableRootQuery<T, R> reverseSorting() {
        TypedQueryData data = this.getData();
        if (data.reverseSorting) {
            return this;
        }
        if (getMutableQuery().getOrders().isEmpty()) {
            return null;
        }
        return new ConfigurableRootQueryImpl<>(
                data.reverseSorting(),
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> setReverseSortOptimizationEnabled(boolean enabled) {
        TypedQueryData data = this.getData();
        if (Objects.equals(data.reverseSortOptimizationEnabled, enabled)) {
            return this;
        }
        return new ConfigurableRootQueryImpl<>(
                data.reverseSortOptimizationEnabled(enabled),
                getMutableQuery()
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
                getMutableQuery()
        );
    }

    @Override
    public ConfigurableRootQuery<T, R> hint(String hint) {
        TypedQueryData data = getData();
        return new ConfigurableRootQueryImpl<>(
                data.hint(hint),
                getMutableQuery()
        );
    }

    @Override
    public List<R> execute(Connection con) {
        return getMutableQuery()
                .getSqlClient()
                .getSlaveConnectionManager(getData().forUpdate)
                .execute(con, this::executeImpl);
    }

    private List<R> executeImpl(Connection con) {
        TypedQueryData data = getData();
        if (data.limit == 0) {
            return Collections.emptyList();
        }
        JSqlClientImplementor sqlClient = getMutableQuery().getSqlClient();
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                data.selections,
                getMutableQuery().getPurpose()
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
        JSqlClientImplementor sqlClient = getMutableQuery().getSqlClient();
        int finalBatchSize = batchSize > 0 ? batchSize : sqlClient.getDefaultBatchSize();
        sqlClient.getSlaveConnectionManager(getData().forUpdate).execute(con, newConn -> {
            forEachImpl(newConn, finalBatchSize, consumer);
            return (Void) null;
        });
    }

    private void forEachImpl(Connection con, int batchSize, Consumer<R> consumer) {
        JSqlClientImplementor sqlClient = getMutableQuery().getSqlClient();
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        Selectors.forEach(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                getData().selections,
                getMutableQuery().getPurpose(),
                batchSize,
                consumer
        );
    }

    private Tuple3<String, List<Object>, List<Integer>> preExecute(SqlBuilder builder) {
        if (!getMutableQuery().isFrozen()) {
            getMutableQuery().applyVirtualPredicates(builder.getAstContext());
            getMutableQuery().applyGlobalFilters(builder.getAstContext(), getMutableQuery().getContext().getFilterLevel(), getData().selections);
        }
        UseTableVisitor visitor = new UseTableVisitor(builder.getAstContext());
        accept(visitor);
        visitor.allocateAliases();
        renderTo(builder);
        return builder.build();
    }

    @Override
    public boolean isForUpdate() {
        return getData().forUpdate;
    }

    @Override
    public TypedRootQuery<R> withLimit(int limit) {
        if (getData().limit == Integer.MAX_VALUE) {
            return limit(limit);
        }
        return this;
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
