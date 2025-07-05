package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherSelection;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class MergedTypedRootQueryImpl<R> implements TypedRootQueryImplementor<R>, TypedQueryImplementor {

    final JSqlClientImplementor sqlClient;

    private final String operator;
    private final List<Selection<?>> selections;
    private final boolean isForUpdate;
    protected final TypedRootQueryImplementor<?>[] queries;

    @SafeVarargs
    public static <R> TypedRootQuery<R> of(String operator, TypedRootQuery<R>... queries) {
        switch (queries.length) {
            case 0:
                throw new IllegalArgumentException("No queries are specified");
            case 1:
                return queries[0];
            default:
                return new MergedTypedRootQueryImpl<>(
                        ((TypedQueryImplementor)queries[0]).getSqlClient(),
                        operator,
                        queries
                );
        }
    }

    private MergedTypedRootQueryImpl(
            JSqlClientImplementor sqlClient,
            String operator,
            TypedRootQuery<R>[] queries) {
        this.sqlClient = sqlClient;
        this.operator = operator;
        if (queries.length < 2) {
            throw new IllegalArgumentException("`queries.length` must not be less than 2");
        }
        TypedRootQueryImplementor<?>[] queryArr = new TypedRootQueryImplementor[queries.length];
        queryArr[0] = (TypedRootQueryImplementor<?>) queries[0];
        List<Selection<?>> selectionArr = null;
        boolean isForUpdate = queryArr[0].isForUpdate();
        for (int i = 1; i < queryArr.length; i++) {
            queryArr[i] = (TypedRootQueryImplementor<?>) queries[i];
            selectionArr = mergedSelections(
                    queryArr[0].getSelections(),
                    queryArr[i].getSelections()
            );
            isForUpdate |= queryArr[i].isForUpdate();
        }
        this.queries = queryArr;
        selections = selectionArr;
        this.isForUpdate = isForUpdate;
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
    public List<R> execute(Connection con) {
        return sqlClient
                .getSlaveConnectionManager(isForUpdate)
                .execute(con, this::executeImpl);
    }

    private List<R> executeImpl(Connection con) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                selections,
                ExecutionPurpose.QUERY
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
        int finalBatchSize = batchSize > 0 ? batchSize : sqlClient.getDefaultBatchSize();
        sqlClient.getSlaveConnectionManager(isForUpdate).execute(con, newConn -> {
            forEachImpl(newConn, finalBatchSize, consumer);
            return (Void) null;
        });
    }

    private void forEachImpl(Connection con, int batchSize, Consumer<R> consumer) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = preExecute(new SqlBuilder(new AstContext(sqlClient)));
        Selectors.forEach(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                sqlResult.get_3(),
                selections,
                ExecutionPurpose.QUERY,
                batchSize,
                consumer
        );
    }

    private Tuple3<String, List<Object>, List<Integer>> preExecute(SqlBuilder builder) {
        UseTableVisitor visitor = new UseTableVisitor(builder.getAstContext());
        accept(visitor);
        visitor.allocateAliases();
        renderTo(builder);
        return builder.build();
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

    @Override
    public boolean isForUpdate() {
        return false;
    }

    @Override
    public TypedRootQuery<R> withLimit(int limit) {
        return this;
    }
}
