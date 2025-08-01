package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.base.AbstractBaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbol;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableSymbols;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.RecursiveRef;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MergedBaseQueryImpl<T extends BaseTable> implements TypedBaseQuery<T>, TypedBaseQueryImplementor<T> {

    private static final ConfigurableBaseQueryImpl<?>[] EMPTY_QUERIES = new ConfigurableBaseQueryImpl[0];

    final JSqlClientImplementor sqlClient;

    private final String operator;

    private TypedBaseQueryImplementor<T>[] queries;

    private ConfigurableBaseQueryImpl<T>[] expandedQueries;

    private T baseTable;

    private MergedBaseQueryImpl<T> mergedBy;

    private final boolean recursive;

    private RecursiveBaseQueryCreator<T>[] recursiveBaseQueryCreators;

    @SafeVarargs
    public static <T extends BaseTable> TypedBaseQuery<T> of(String operator, TypedBaseQuery<T> ... queries) {
        switch (queries.length) {
            case 0:
                throw new IllegalArgumentException("No queries are specified");
            case 1:
                return queries[0];
            default:
                return new MergedBaseQueryImpl<>(
                        ((TypedBaseQueryImplementor<?>)queries[0]).getSqlClient(),
                        operator,
                        queries,
                        null
                );
        }
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T extends BaseTable> TypedBaseQuery<T> of(
            TypedBaseQuery<T> query,
            RecursiveBaseQueryCreator<T> ... recursiveBaseQueryCreators
    ) {
        if (recursiveBaseQueryCreators.length == 0) {
            return query;
        }
        return new MergedBaseQueryImpl<>(
                ((TypedBaseQueryImplementor<?>) query).getSqlClient(),
                "union all",
                (TypedBaseQuery<T>[]) new TypedBaseQuery<?>[] {query},
                recursiveBaseQueryCreators
        );
    }

    @SuppressWarnings("unchecked")
    private MergedBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedBaseQuery<T>[] queries,
            RecursiveBaseQueryCreator<T>[] recursiveBaseQueryCreators
    ) {
        for (TypedBaseQuery<?> query : queries) {
            ((TypedBaseQueryImplementor<T>)query).setMergedBy(this);
        }

        this.sqlClient = sqlClient;
        this.operator = operator;

        if (recursiveBaseQueryCreators != null) {
            if (recursiveBaseQueryCreators.length < 1) {
                throw new IllegalArgumentException("`recursiveBaseQueryCreators.length` must not be less than 2");
            }
            if (queries.length < 1) {
                throw new IllegalArgumentException("no start query for recursive CTE");
            }
        } else if (queries.length < 2) {
            throw new IllegalArgumentException("`queries.length` must not be less than 2");
        }
        TypedBaseQueryImplementor<T>[] queryArr = new TypedBaseQueryImplementor[queries.length];
        queryArr[0] = (TypedBaseQueryImplementor<T>) queries[0];
        for (int i = 1; i < queryArr.length; i++) {
            queryArr[i] = (TypedBaseQueryImplementor<T>) queries[i];
            validateSelections(
                    queryArr[0].getSelections(),
                    queryArr[i].getSelections()
            );
        }
        this.queries = queryArr;

        List<ConfigurableBaseQueryImpl<?>> realQueries = new ArrayList<>();
        collectRealQueries(this, realQueries);
        this.expandedQueries = (ConfigurableBaseQueryImpl<T>[]) realQueries.toArray(EMPTY_QUERIES);

        this.recursive = recursiveBaseQueryCreators != null;
        this.recursiveBaseQueryCreators = recursiveBaseQueryCreators;
    }

    @SuppressWarnings("unchecked")
    private void upgrade() {
        if (recursiveBaseQueryCreators == null) {
            return;
        }
        T baseTable = asBaseTable(null, true);
        int size = queries.length;
        TypedBaseQueryImplementor<T>[] newQueryArr = new TypedBaseQueryImplementor[size + recursiveBaseQueryCreators.length];
        System.arraycopy(queries, 0, newQueryArr, 0, size);
        for (RecursiveBaseQueryCreator<T> creator : recursiveBaseQueryCreators) {
            TypedBaseQueryImplementor<T> query = (TypedBaseQueryImplementor<T>) creator.create(
                    BaseTableSymbols.recursive(baseTable)
            );
            validateSelections(
                    queries[0].getSelections(),
                    query.getSelections()
            );
            newQueryArr[size++] = query;
        }
        this.recursiveBaseQueryCreators = null;
        this.queries = newQueryArr;
        List<ConfigurableBaseQueryImpl<?>> realQueries = new ArrayList<>();
        collectRealQueries(this, realQueries);
        this.expandedQueries = (ConfigurableBaseQueryImpl<T>[]) realQueries.toArray(EMPTY_QUERIES);
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

    public boolean isRecursive() {
        return recursive;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.getAstContext().visitRecursiveQuery(this, () -> {
            for (ConfigurableBaseQueryImpl<?> query : getExpandedQueries()) {
                query.accept(visitor);
            }
        });
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter('?' + operator + '?');
        for (TypedQueryImplementor query : getQueries()) {
            builder.separator();
            query.renderTo(builder);
        }
        builder.leave();
    }

    @Override
    public boolean hasVirtualPredicate() {
        for (TypedQueryImplementor query : getQueries()) {
            if (query.hasVirtualPredicate()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        TypedBaseQueryImplementor<T>[] queries = getQueries();
        for (int i = 0; i < queries.length; i++) {
            queries[i] = ctx.resolveVirtualPredicate(queries[i]);
        }
        return this;
    }

    @Override
    public List<Selection<?>> getSelections() {
        return ((BaseTableSymbol) baseTable).getSelections();
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    public TypedBaseQueryImplementor<T>[] getQueries() {
        upgrade();
        return queries;
    }

    public ConfigurableBaseQueryImpl<T>[] getExpandedQueries() {
        upgrade();
        return expandedQueries;
    }

    public ConfigurableBaseQueryImpl<T> firstQuery() {
        return expandedQueries[0];
    }

    @Override
    public TableImplementor<?> resolveRootTable(Table<?> table) {
        for (TypedBaseQueryImplementor<?> query : this.getQueries()) {
            TableImplementor<?> tableImplementor;
            if (query instanceof MergedBaseQueryImpl<?>) {
                tableImplementor = ((MergedBaseQueryImpl<?>)query).getQueries()[0].resolveRootTable(table);
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
        if (this.baseTable != null) {
            throw new IllegalStateException(
                    "The base query cannot be merged after its `asBaseTable` is called"
            );
        }
        if (this.mergedBy != null && this.mergedBy != mergedBy) {
            throw new IllegalArgumentException(
                    "This current base-query has been merged by another merged base query"
            );
        }
        this.mergedBy = mergedBy;
    }

    @Override
    public T asBaseTable() {
        return asBaseTable(null, false);
    }

    @Override
    public T asCteBaseTable() {
        return asBaseTable(null, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T asBaseTable(byte[] kotlinSelectionTypes, boolean cte) {
        if (!cte && recursive) {
            throw new IllegalArgumentException(
                    "Recursive base query only be treated as cteBaseTable, not general baseTable"
            );
        }
        T baseTable = this.baseTable;
        if (baseTable != null) {
            return AbstractBaseTableSymbol.validateCte(baseTable, cte);
        }
        this.baseTable = baseTable =
                mergedBy != null ?
                        mergedBy.asBaseTable(kotlinSelectionTypes, cte) :
                        (T) BaseTableSymbols.of(this, expandedQueries[0].getSelections(), kotlinSelectionTypes, cte);
        return baseTable;
    }

    private static void collectRealQueries(
            TypedBaseQueryImplementor<?> query,
            List<ConfigurableBaseQueryImpl<?>> results
    ) {
        if (query instanceof ConfigurableBaseQuery<?>) {
            results.add((ConfigurableBaseQueryImpl<?>) query);
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
