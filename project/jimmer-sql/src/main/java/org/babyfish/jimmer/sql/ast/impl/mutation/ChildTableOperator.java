package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ChildTableOperator {

    private JSqlClient sqlClient;

    private Connection con;

    private ImmutableProp parentProp;

    public ChildTableOperator(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp parentProp
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.parentProp = parentProp;
    }

    public int setParent(Object parentId, Collection<Object> childIds) {

        if (childIds.isEmpty()) {
            return 0;
        }

        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("update ")
                .sql(parentProp.getDeclaringType().getTableName())
                .sql(" set ")
                .sql(parentProp.<Column>getStorage().getName())
                .sql(" = ")
                .variable(parentId)
                .sql(" where ")
                .sql(parentProp.getDeclaringType().getIdProp().<Column>getStorage().getName())
                .sql(" in(");
        String separator = "";
        for (Object childId : childIds) {
            builder.sql(separator);
            separator = ", ";
            builder.variable(childId);
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                PreparedStatement::executeUpdate
        );
    }

    public int unsetParent(Object parentId, Collection<Object> retainedChildIds) {
        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("update ")
                .sql(parentProp.getDeclaringType().getTableName())
                .sql(" set ")
                .sql(parentProp.<Column>getStorage().getName())
                .sql(" = null");
        addDetachConditions(builder, parentId, retainedChildIds);

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                PreparedStatement::executeUpdate
        );
    }

    public List<Object> getDetachedChildIds(Object parentId, Collection<Object> retainedChildIds) {
        SqlBuilder builder = new SqlBuilder(sqlClient);
        ImmutableProp idProp = parentProp.getDeclaringType().getIdProp();
        builder
                .sql("select ")
                .sql(idProp.<Column>getStorage().getName())
                .sql(" from ")
                .sql(parentProp.getDeclaringType().getTableName());
        addDetachConditions(builder, parentId, retainedChildIds);

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                ExecutionPurpose.MUTATE,
                null,
                stmt -> {
                    List<Object> list = new ArrayList<>();
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Object value = rs.getObject(1);
                            Object id = Converters.tryConvert(value, idProp.getElementClass());
                            if (id == null) {
                                throw new ExecutionException(
                                        "Cannot convert " + value + " to the type of " + idProp
                                );
                            }
                            list.add(id);
                        }
                    }
                    return list;
                }
        );
    }

    private void addDetachConditions(
            SqlBuilder builder,
            Object parentId,
            Collection<Object> retainedChildIds
    ) {
        builder
                .sql(" where ")
                .sql(parentProp.<Column>getStorage().getName())
                .sql(" = ")
                .variable(parentId);
        if (!retainedChildIds.isEmpty()) {
            builder
                    .sql(" and ")
                    .sql(parentProp.getDeclaringType().getIdProp().<Column>getStorage().getName())
                    .sql(" not in(");
            String separator = "";
            for (Object retainedChildId : retainedChildIds) {
                builder.sql(separator);
                separator = ", ";
                builder.variable(retainedChildId);
            }
            builder.sql(")");
        }
    }
}
