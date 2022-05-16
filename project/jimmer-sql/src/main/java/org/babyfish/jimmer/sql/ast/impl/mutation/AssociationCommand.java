package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.util.*;

class AssociationCommand implements Executable<Integer> {

    private SqlClient sqlClient;

    private AssociationType associationType;

    private boolean reversed;

    private Mode mode;

    private Set<Tuple2<Object, Object>> idPairs;

    public AssociationCommand(
            SqlClient sqlClient,
            AssociationType associationType,
            boolean reversed,
            Mode mode,
            Collection<Tuple2<Object, Object>> idPairs
    ) {
        this.sqlClient = sqlClient;
        this.associationType = associationType;
        this.reversed = reversed;
        this.mode = mode;
        this.idPairs = idPairs instanceof Set<?> ?
                (Set<Tuple2<Object, Object>>)idPairs :
                new LinkedHashSet<>(idPairs);
    }

    @Override
    public Integer execute(Connection con) {

        if (idPairs.isEmpty()) {
            return 0;
        }

        if (mode == Mode.DELETE) {
            return getMiddleTypeOperator(con).remove(new TupleReader(idPairs));
        }

        Set<Tuple2<Object, Object>> addingPairs = idPairs;
        if (mode == Mode.CHECK_AND_INSERT) {
            addingPairs = new LinkedHashSet<>(addingPairs);
            Set<Tuple2<Object, Object>> existingPairs = new HashSet<>(find(con));
            addingPairs.removeAll(existingPairs);
            if (addingPairs.isEmpty()) {
                return 0;
            }
        }
        return getMiddleTypeOperator(con).add(new TupleReader(addingPairs));
    }

    public enum Mode {
        CHECK_AND_INSERT,
        INSERT,
        DELETE
    }

    private List<Tuple2<Object, Object>> find(Connection con) {

        MiddleTable middleTable = reversed ?
                associationType.getMiddleTable().getInverse() :
                associationType.getMiddleTable();
        Tuple2<Expression<?>, Expression<?>> expressionPair = getExpressionPair();

        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("select ")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(" from ")
                .sql(associationType.getTableName())
                .sql(" where (")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") in(");
        String separator = "";
        for (Tuple2<Object, Object> idPair : idPairs) {
            builder
                    .sql(separator)
                    .sql("(")
                    .variable(idPair._1())
                    .sql(", ")
                    .variable(idPair._2())
                    .sql(")");
            separator = ", ";
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult._1(),
                sqlResult._2(),
                Arrays.asList(expressionPair._1(), expressionPair._2())
        );
    }

    private Tuple2<Expression<?>, Expression<?>> getExpressionPair() {
        Class<?> srcType = associationType.getSourceType().getIdProp().getElementClass();
        Class<?> tgtType = associationType.getTargetType().getIdProp().getElementClass();
        if (reversed) {
            return new Tuple2<>(
                    Expression.any().nullValue(tgtType),
                    Expression.any().nullValue(srcType)
            );
        }
        return new Tuple2<>(
                Expression.any().nullValue(srcType),
                Expression.any().nullValue(tgtType)
        );
    }

    private MiddleTableOperator getMiddleTypeOperator(Connection con) {
        return new MiddleTableOperator(
                sqlClient,
                con,
                reversed ?
                        associationType.getMiddleTable().getInverse() :
                        associationType.getMiddleTable(),
                (reversed ? associationType.getSourceType() : associationType.getTargetType())
                        .getIdProp()
                        .getElementClass()
        );
    }

    private static class TupleReader implements MiddleTableOperator.IdPairReader {

        private Iterator<Tuple2<Object, Object>> idPairItr;

        private Tuple2<Object, Object> currentIdPair;

        TupleReader(Collection<Tuple2<Object, Object>> idPairs) {
            idPairItr = idPairs.iterator();
        }

        @Override
        public boolean read() {
            if (idPairItr.hasNext()) {
                currentIdPair = idPairItr.next();
                return true;
            }
            return false;
        }

        @Override
        public Object sourceId() {
            return currentIdPair._1();
        }

        @Override
        public Object targetId() {
            return currentIdPair._2();
        }
    }
}
