package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

class MiddleTableOperator {

    private SqlClient sqlClient;

    private Connection con;

    private MiddleTable middleTable;

    private Expression<?> targetIdExpression;

    public MiddleTableOperator(
            SqlClient sqlClient,
            Connection con,
            MiddleTable middleTable,
            Class<?> targetIdType
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.middleTable = middleTable;
        this.targetIdExpression = Expression.any().nullValue(targetIdType);
    }

    public List<Object> getTargetIds(Object id) {
        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("select ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(" from ")
                .sql(middleTable.getTableName())
                .sql(" where ")
                .sql(middleTable.getJoinColumnName())
                .sql(" = ")
                .variable(id);
        Tuple2<String, List<Object>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult._1(),
                sqlResult._2(),
                Collections.singletonList(targetIdExpression)
        );
    }

    public int addTargetIds(Object id, Collection<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return 0;
        }
        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("insert into ")
                .sql(middleTable.getTableName())
                .sql("(")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") values ");
        String separator = "";
        for (Object targetId : set) {
            builder.sql(separator);
            separator = ", ";
            builder
                    .sql("(")
                    .variable(id)
                    .sql(", ")
                    .variable(targetId)
                    .sql(")");
        }
        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult._1(),
                sqlResult._2(),
                PreparedStatement::executeUpdate
        );
    }

    public int removeTargetIds(Object id, Collection<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return 0;
        }
        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .sql(" where (")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") in (");
        String separator = "";
        for (Object targetId : set) {
            builder.sql(separator);
            separator = ", ";
            builder
                    .sql("(")
                    .variable(id)
                    .sql(", ")
                    .variable(targetId)
                    .sql(")");
        }
        builder.sql(")");
        Tuple2<String, List<Object>> sqlResult = builder.build();
        return sqlClient.getExecutor().execute(
                con,
                sqlResult._1(),
                sqlResult._2(),
                PreparedStatement::executeUpdate
        );
    }

    public int setTargetIds(Object id, Collection<Object> targetIds) {

        Set<Object> oldTargetIds = new LinkedHashSet<>(getTargetIds(id));

        Set<Object> addingTargetIds = new LinkedHashSet<>(targetIds);
        addingTargetIds.removeAll(oldTargetIds);

        Set<Object> removingTargetIds = new LinkedHashSet<>(oldTargetIds);
        removingTargetIds.removeAll(targetIds);

        return removeTargetIds(id, removingTargetIds) + addTargetIds(id, addingTargetIds);
    }
}
