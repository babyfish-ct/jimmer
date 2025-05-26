package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.SimpleBaseTables;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MergedBaseQueryImpl<T extends BaseTable> implements TypedBaseQuery<T>, TypedBaseQueryImplementor<T> {

    final JSqlClientImplementor sqlClient;

    private final String operator;
    private final List<Selection<?>> selections;
    private TypedBaseQueryImplementor<?>[] queries;
    private final T baseTable;

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

    @SuppressWarnings("unchecked")
    @SafeVarargs
    MergedBaseQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedBaseQuery<T>... queries
    ) {
        this.sqlClient = sqlClient;
        this.operator = operator;
        if (queries.length < 2) {
            throw new IllegalArgumentException("`queries.length` must not be less than 2");
        }
        TypedBaseQueryImplementor<?>[] queryArr = new TypedBaseQueryImplementor<?>[queries.length];
        queryArr[0] = (TypedBaseQueryImplementor<?>) queries[0];
        List<Selection<?>> selectionArr = null;
        for (int i = 1; i < queryArr.length; i++) {
            queryArr[i] = (TypedBaseQueryImplementor<?>) queries[i];
            selectionArr = mergedSelections(
                    queryArr[0].getSelections(),
                    queryArr[i].getSelections()
            );
        }
        this.queries = queryArr;
        selections = selectionArr;
        baseTable = (T) SimpleBaseTables.of(this, toConfigurableBaseQuery().getSelections());
    }

    private static List<Selection<?>> mergedSelections(
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
        return list1;
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
        for (TypedQueryImplementor query : queries) {
            query.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter('?' + operator + '?');
        for (TypedQueryImplementor query : queries) {
            builder.separator();
            builder.sql("(");
            query.renderTo(builder);
            builder.sql(")");
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
    public T asBaseTable() {
        return baseTable;
    }

    @SuppressWarnings("unchecked")
    public ConfigurableBaseQueryImpl<T> toConfigurableBaseQuery() {
        TypedBaseQueryImplementor<?> query = queries[0];
        if (query instanceof ConfigurableBaseQuery<?>) {
            return (ConfigurableBaseQueryImpl<T>) query;
        }
        return ((MergedBaseQueryImpl<T>)query).toConfigurableBaseQuery();
    }
}
