package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.BaseTableImplementor;
import org.babyfish.jimmer.sql.ast.impl.base.MergedBaseTableImplementor;
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
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.*;

public class MergedBaseQueryImpl<T extends BaseTable> implements TypedBaseQuery<T>, TypedBaseQueryImplementor<T> {

    private static final Class<?>[] EMPTY_CLASSES = new Class[0];

    final JSqlClientImplementor sqlClient;

    private final String operator;

    private final TypedBaseQueryImplementor<?>[] queries;

    private final List<Selection<?>> selections;

    private final T baseTable;

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
        int selectionCount = queryArr[0].getSelections().size();
        List<Selection<?>> selections = new ArrayList<>();
        for (int i = 0; i < selectionCount; i++) {
            selections.add(newSelectionProxy(i, Arrays.asList(queryArr)));
        }
        this.selections = selections;

        Set<BaseTableImplementor> baseTables = new LinkedHashSet<>();
        for (TypedBaseQueryImplementor<?> query : queryArr) {
            BaseTableImplementor baseTable = (BaseTableImplementor) query.asBaseTable();
            if (baseTable instanceof MergedBaseTableImplementor) {
                baseTables.addAll(((MergedBaseTableImplementor)baseTable).getBaseTables());
            } else {
                baseTables.add(baseTable);
            }
        }
        this.baseTable = newBaseTableProxy(Collections.unmodifiableSet(baseTables), selections);
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

    private static Selection<?> newSelectionProxy(int index, List<TypedBaseQueryImplementor<?>> queries) {
        List<Selection<?>> selections = new ArrayList<>(queries.size());
        Set<Class<?>> interfaceTypes = null;
        for (TypedBaseQueryImplementor<?> query : queries) {
            Selection<?> selection = query.getSelections().get(index);
            selections.add(selection);
            Set<Class<?>> itfTypes = new LinkedHashSet<>();
            collectInterfaces(selection.getClass(), itfTypes);
            if (interfaceTypes == null) {
                interfaceTypes = itfTypes;
            } else {
                interfaceTypes.retainAll(itfTypes);
            }
        }
        assert interfaceTypes != null;
        ClassLoader classLoader = interfaceTypes.iterator().next().getClassLoader();
        return (Selection<?>) Proxy.newProxyInstance(
                classLoader,
                interfaceTypes.toArray(EMPTY_CLASSES),
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "renderTo":
                            AbstractSqlBuilder<?> builder = (AbstractSqlBuilder<?>) args[0];
                            for (Selection<?> selection : selections) {
                                method.invoke(selection, args);
                                args[0] = new SqlBuilder(builder.assertSimple().getAstContext());
                            }
                            return null;
                        default:
                            Object result = null;
                            for (Selection<?> selection : selections) {
                                result = method.invoke(selection, args);
                            }
                            return result;
                    }
                }
        );
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> types) {
        if (type.isInterface()) {
            types.add(type);
        }
        Class<?> superType = type.getSuperclass();
        if (superType != null && superType != Object.class) {
            collectInterfaces(superType, types);
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            collectInterfaces(interfaceType, types);
        }
    }

    @SuppressWarnings("unchecked")
    private T newBaseTableProxy(Set<BaseTableImplementor> baseTables, List<?> selections) {
        BaseTableImplementor firstBaseTable = baseTables.iterator().next();
        Set<Class<?>> interfaces = new LinkedHashSet<>(Arrays.asList(firstBaseTable.getClass().getInterfaces()));
        interfaces.add(MergedBaseTableImplementor.class);
        return (T) Proxy.newProxyInstance(
                firstBaseTable.getClass().getClassLoader(),
                interfaces.toArray(EMPTY_CLASSES),
                ((proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSelections":
                            return selections;
                        case "getBaseTables":
                            return baseTables;
                        case "getQuery":
                            return this;
                        case "getStatement":
                            return new IllegalStateException("Merged Base Table does not support \"getStatement\"");
                        case "accept":
                            accept((AstVisitor) args[0]);
                            return null;
                        case "renderTo":
                            AbstractSqlBuilder<?> builder = (AbstractSqlBuilder<?>) args[0];
                            builder.sql(" from ");
                            renderTo(builder);
                            builder
                                    .sql(" ")
                                    .sql(
                                            ((BaseTableImplementor)proxy)
                                                    .realTable(builder.assertSimple().getAstContext().getJoinTypeMergeScope())
                                                    .getAlias()
                                    );
                            return null;
                        default:
                            return method.invoke(firstBaseTable, args);
                    }
                })
        );
    }
}
