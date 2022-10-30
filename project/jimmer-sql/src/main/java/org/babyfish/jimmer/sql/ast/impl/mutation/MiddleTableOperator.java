package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

class MiddleTableOperator {

    private JSqlClient sqlClient;

    private Connection con;

    private MiddleTable middleTable;

    private Expression<?> targetIdExpression;

    MiddleTableOperator(
            JSqlClient sqlClient,
            Connection con,
            MiddleTable middleTable,
            Class<?> targetIdType
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.middleTable = middleTable;
        this.targetIdExpression = Expression.any().nullValue(targetIdType);
    }

    List<Object> getTargetIds(Object id) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
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
                sqlResult.get_1(),
                sqlResult.get_2(),
                Collections.singletonList(targetIdExpression),
                ExecutionPurpose.MUTATE
        );
    }

    int addTargetIds(Object id, Collection<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return 0;
        }
        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        return add(new OneToManyReader(id, set));
    }

    int add(IdPairReader reader) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("insert into ")
                .sql(middleTable.getTableName())
                .sql("(")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") values ");
        String separator = "";
        while (reader.read()) {
            builder.sql(separator);
            separator = ", ";
            builder
                    .sql("(")
                    .variable(reader.sourceId())
                    .sql(", ")
                    .variable(reader.targetId())
                    .sql(")");
        }
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

    int removeTargetIds(Object id, Collection<Object> targetIds) {
        if (targetIds.isEmpty()) {
            return 0;
        }
        Set<Object> set = targetIds instanceof Set<?> ?
                (Set<Object>)targetIds :
                new LinkedHashSet<>(targetIds);
        return remove(new OneToManyReader(id, set));
    }

    int remove(IdPairReader reader) {
        SqlBuilder builder = new SqlBuilder(new AstContext(sqlClient));
        builder
                .sql("delete from ")
                .sql(middleTable.getTableName())
                .sql(" where (")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") in (");
        String separator = "";
        while (reader.read()) {
            builder.sql(separator);
            separator = ", ";
            builder
                    .sql("(")
                    .variable(reader.sourceId())
                    .sql(", ")
                    .variable(reader.targetId())
                    .sql(")");
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

    int setTargetIds(Object id, Collection<Object> targetIds) {

        Set<Object> oldTargetIds = new LinkedHashSet<>(getTargetIds(id));

        Set<Object> addingTargetIds = new LinkedHashSet<>(targetIds);
        addingTargetIds.removeAll(oldTargetIds);

        Set<Object> removingTargetIds = new LinkedHashSet<>(oldTargetIds);
        removingTargetIds.removeAll(targetIds);

        return removeTargetIds(id, removingTargetIds) + addTargetIds(id, addingTargetIds);
    }

    interface IdPairReader {
        boolean read();
        Object sourceId();
        Object targetId();
    }

    private static class OneToManyReader implements IdPairReader {

        private Object sourceId;

        private Iterator<Object> targetIdItr;

        private Object currentTargetId;

        private int index = -1;

        OneToManyReader(Object sourceId, Collection<Object> targetIds) {
            this.sourceId = sourceId;
            this.targetIdItr = targetIds.iterator();
        }

        @Override
        public boolean read() {
            if (targetIdItr.hasNext()) {
                currentTargetId = targetIdItr.next();
                return true;
            }
            return false;
        }

        @Override
        public Object sourceId() {
            return sourceId;
        }

        @Override
        public Object targetId() {
            return currentTargetId;
        }
    }
}
