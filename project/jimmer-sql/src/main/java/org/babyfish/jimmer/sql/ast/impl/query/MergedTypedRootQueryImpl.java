package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableWrappers;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.List;
import java.util.function.Consumer;

class MergedTypedRootQueryImpl<R> implements TypedRootQueryImplementor<R>, TypedQueryImplementor {

    private JSqlClient sqlClient;

    private String operator;

    private TypedQueryImplementor left;

    private TypedQueryImplementor right;

    private List<Selection<?>> selections;

    private boolean isForUpdate;

    public MergedTypedRootQueryImpl(
            JSqlClient sqlClient,
            String operator,
            TypedRootQuery<R> left,
            TypedRootQuery<R> right) {
        this.sqlClient = sqlClient;
        this.operator = operator;
        this.left = (TypedQueryImplementor) left;
        this.right = (TypedQueryImplementor) right;
        selections = mergedSelections(
                this.left.getSelections(),
                this.right.getSelections()
        );
        isForUpdate = ((TypedRootQueryImplementor<?>)left).isForUpdate() ||
                ((TypedRootQueryImplementor<?>)right).isForUpdate();
    }

    @Override
    public List<R> execute() {
        return sqlClient
                .getSlaveConnectionManager(isForUpdate)
                .execute(this::executeImpl);
    }

    @Override
    public List<R> execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getSlaveConnectionManager(isForUpdate)
                .execute(this::executeImpl);
    }

    private List<R> executeImpl(Connection con) {
        Tuple2<String, List<Object>> sqlResult = preExecute(new SqlBuilder(sqlClient));
        return Selectors.select(sqlClient, con, sqlResult.get_1(), sqlResult.get_2(), selections);
    }

    @Override
    public void forEach(Connection con, int batchSize, Consumer<R> consumer) {
        int finalBatchSize = batchSize > 0 ? batchSize : sqlClient.getDefaultBatchSize();
        if (con != null) {
            forEachImpl(con, finalBatchSize, consumer);
        } else {
            sqlClient.getSlaveConnectionManager(isForUpdate).execute(newConn -> {
                forEachImpl(newConn, finalBatchSize, consumer);
                return (Void) null;
            });
        }
    }

    private void forEachImpl(Connection con, int batchSize, Consumer<R> consumer) {
        Tuple2<String, List<Object>> sqlResult = preExecute(new SqlBuilder(sqlClient));
        Selectors.forEach(sqlClient, con, sqlResult.get_1(), sqlResult.get_2(), selections, batchSize, consumer);
    }

    private Tuple2<String, List<Object>> preExecute(SqlBuilder builder) {
        AstVisitor visitor = new UseTableVisitor(builder);
        accept(visitor);
        renderTo(builder);
        return builder.build();
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        left.accept(visitor);
        right.accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        left.renderTo(builder);
        builder.sql(" ");
        builder.sql(operator);
        builder.sql(" ");
        right.renderTo(builder);
    }

    @Override
    public List<Selection<?>> getSelections() {
        return selections;
    }

    @Override
    public TypedRootQuery<R> union(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(sqlClient, "union", this, other);
    }

    @Override
    public TypedRootQuery<R> unionAll(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(sqlClient, "union all", this, other);
    }

    @Override
    public TypedRootQuery<R> minus(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(sqlClient, "minus", this, other);
    }

    @Override
    public TypedRootQuery<R> intersect(TypedRootQuery<R> other) {
        return new MergedTypedRootQueryImpl<>(sqlClient, "intersect", this, other);
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
        if (a instanceof Table<?> && b instanceof Table<?>) {
            return TableWrappers.unwrap((Table<?>) a).getImmutableType() ==
                    TableWrappers.unwrap((Table<?>) b).getImmutableType();
        }
        if (a instanceof Expression<?> && b instanceof Expression<?>) {
            return ((ExpressionImplementor<?>) a).getType() ==
                    ((ExpressionImplementor<?>) b).getType();
        }
        return false;
    }

    @Override
    public boolean isForUpdate() {
        return false;
    }
}
