package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;

public class MergedBaseQueryImpl<T extends BaseTable> implements TypedBaseQuery<T>, TypedBaseQueryImplementor<T> {

    private static final Class<?>[] EMPTY_CLASSES = new Class[0];

    private static final ConfigurableBaseQueryImpl<?>[] EMPTY_QUERIES = new ConfigurableBaseQueryImpl[0];

    final JSqlClientImplementor sqlClient;

    private final String operator;

    private final TypedBaseQueryImplementor<?>[] queries;

    private final ConfigurableBaseQueryImpl<?>[] expandedQueries;

    private final List<Selection<?>> selections;

    private final T baseTable;

    private MergedBaseQueryImpl<T> mergedBy;

    @SafeVarargs
    public static <T extends BaseTable> TypedBaseQuery<T> of(String operator, TypedBaseQuery<T> ... queries) {
        switch (queries.length) {
            case 0:
                return null;
            case 1:
                return queries[0];
            default:
                return new MergedBaseQueryImpl<>(
                        ((TypedBaseQueryImplementor<?>)queries[0]).getSqlClient(),
                        operator,
                        queries
                );
        }
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    MergedBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedBaseQuery<T>... queries
    ) {
        for (TypedBaseQuery<?> query : queries) {
            ((TypedBaseQueryImplementor<T>)query).setMergedBy(this);
        }

        this.sqlClient = sqlClient;
        this.operator = operator;

        if (queries.length < 2) {
            throw new IllegalArgumentException("`queries.length` must not be less than 2");
        }
        TypedBaseQueryImplementor<?>[] queryArr = new TypedBaseQueryImplementor<?>[queries.length];
        queryArr[0] = (TypedBaseQueryImplementor<?>) queries[0];
        for (int i = 1; i < queryArr.length; i++) {
            queryArr[i] = (TypedBaseQueryImplementor<?>) queries[i];
            if (queries[0].asBaseTable().getClass() != queries[i].asBaseTable().getClass()) {
                throw new IllegalArgumentException(
                        "Cannot merged sub queries with different base table type"
                );
            }
            validateSelections(
                    queryArr[0].getSelections(),
                    queryArr[i].getSelections()
            );
        }
        this.queries = queryArr;

        List<TypedBaseQueryImplementor<?>> realQueries = new ArrayList<>();
        collectRealQueries(this, realQueries);
        this.selections = realQueries.get(0).getSelections();

        List<ConfigurableBaseQueryImpl<?>> expandedQueries = new ArrayList<>();
        for (TypedBaseQueryImplementor<?> query : queryArr) {
            if (query instanceof MergedBaseQueryImpl<?>) {
                expandedQueries.addAll(Arrays.asList(((MergedBaseQueryImpl<?>) query).expandedQueries));
            } else {
                expandedQueries.add((ConfigurableBaseQueryImpl<?>) query);
            }
        }
        this.expandedQueries = expandedQueries.toArray(EMPTY_QUERIES);

        this.baseTable = (T) expandedQueries.get(0).asBaseTable();
    }

    private static void validateSelections(
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
    }

    private static boolean isSameType(Selection<?> a, Selection<?> b) {
        if (a instanceof TableTypeProvider && b instanceof TableTypeProvider) {
            return ((TableTypeProvider) a).getImmutableType() == ((TableTypeProvider) b).getImmutableType();
        }
        if (a instanceof FetcherSelection<?> && b instanceof FetcherSelection<?>) {
            return ((FetcherSelection<?>) a).getFetcher().equals(((FetcherSelection<?>) b).getFetcher());
        }
        if (a instanceof Expression<?> && b instanceof Expression<?>) {
            return ((ExpressionImplementor<?>) a).getType() ==
                    ((ExpressionImplementor<?>) b).getType();
        }
        return false;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        for (ConfigurableBaseQueryImpl<?> query : expandedQueries) {
            query.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter('?' + operator + '?');
        for (TypedQueryImplementor query : queries) {
            builder.separator();
            query.renderTo(builder);
        }
        builder.leave();
    }

    @Override
    public boolean hasVirtualPredicate() {
        for (TypedQueryImplementor query : queries) {
            if (query.hasVirtualPredicate()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        for (int i = 0; i < queries.length; i++) {
            queries[i] = ctx.resolveVirtualPredicate(queries[i]);
        }
        return this;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    public TypedBaseQueryImplementor<?>[] getQueries() {
        return queries;
    }

    public TypedBaseQueryImplementor<?>[] getExpandedQueries() {
        return expandedQueries;
    }

    @Override
    public TableImplementor<?> resolveRootTable(Table<?> table) {
        for (TypedBaseQueryImplementor<?> query : this.queries) {
            TableImplementor<?> tableImplementor;
            if (query instanceof MergedBaseQueryImpl<?>) {
                tableImplementor = ((MergedBaseQueryImpl<?>)query).queries[0].resolveRootTable(table);
            } else {
                MutableBaseQueryImpl mutableQuery = ((ConfigurableBaseQueryImpl<?>) query).getMutableQuery();
                tableImplementor = AbstractTypedTable.__refEquals(mutableQuery.getTable(), table) ?
                        (TableImplementor<?>) mutableQuery.getTableLikeImplementor() :
                        null;
            }
            if (tableImplementor != null) {
                return tableImplementor;
            }
        }
        return null;
    }

    @Override
    public MergedBaseQueryImpl<T> getMergedBy() {
        return mergedBy;
    }

    @Override
    public void setMergedBy(MergedBaseQueryImpl<T> mergedBy) {
        if (this.mergedBy != null && this.mergedBy != mergedBy) {
            throw new IllegalArgumentException(
                    "This current base-query has been merged by another merged base query"
            );
        }
        this.mergedBy = mergedBy;
    }

    @Override
    public T asBaseTable() {
        return baseTable;
    }

    private static void collectRealQueries(
            TypedBaseQueryImplementor<?> query,
            List<TypedBaseQueryImplementor<?>> results
    ) {
        if (query instanceof ConfigurableBaseQuery<?>) {
            results.add(query);
        } else {
            MergedBaseQueryImpl<?> mq = (MergedBaseQueryImpl<?>)query;
            for (TypedBaseQueryImplementor<?> sq : mq.getQueries()) {
                collectRealQueries(sq, results);
            }
        }
    }

    public static MergedBaseQueryImpl<?> from(TypedBaseQueryImplementor<?> query) {
        return from0(query, null);
    }

    private static MergedBaseQueryImpl<?> from0(
            TypedBaseQueryImplementor<?> query,
            MergedBaseQueryImpl<?> prevMergedBy
    ) {
        MergedBaseQueryImpl<?> mergedBy = query.getMergedBy();
        if (mergedBy == null) {
            return prevMergedBy;
        }
        return from0(mergedBy, mergedBy);
    }
}
