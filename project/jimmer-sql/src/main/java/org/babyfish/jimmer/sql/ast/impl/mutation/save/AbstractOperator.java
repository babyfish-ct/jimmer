package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

abstract class AbstractOperator {

    final JSqlClientImplementor sqlClient;

    final Connection con;

    AbstractOperator(JSqlClientImplementor sqlClient, Connection con) {
        this.sqlClient = sqlClient;
        this.con = con;
    }

    final <R> R execute(SqlBuilder builder, SqlFunction<PreparedStatement, R> statementExecutor) {
        Tuple3<String, List<Object>, List<Integer>> sqlResult = builder.build();
        return sqlClient
                .getExecutor()
                .execute(
                        new Executor.Args<>(
                                sqlClient,
                                con,
                                sqlResult.get_1(),
                                sqlResult.get_2(),
                                sqlResult.get_3(),
                                ExecutionPurpose.MUTATE,
                                null,
                                statementExecutor
                        )
                );
    }

    int execute(SqlBuilder builder) {
        return execute(builder, PreparedStatement::executeUpdate);
    }

    final int execute(BatchSqlBuilder builder, Collection<?> rows) {
        int[] rowCounts = executeImpl(builder, rows);
        int sumRowCount = 0;
        if (isBatchStatementSimple()) {
            for (int rowCount : rowCounts) {
                if (rowCount != 0) {
                    sumRowCount++;
                }
            }
        } else {
            for (int rowCount : rowCounts) {
                sumRowCount += rowCount;
            }
        }
        return sumRowCount;
    }

    int[] executeImpl(BatchSqlBuilder builder, Collection<?> rows) {
        Tuple2<String, BatchSqlBuilder.VariableMapper> sqlTuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor().executeBatch(
                        sqlClient,
                        con,
                        sqlTuple.get_1(),
                        null
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = sqlTuple.get_2();
            for (Object row : rows) {
                batchContext.add(mapper.variables(row));
            }
            return batchContext.execute();
        }
    }

    boolean isBatchStatementSimple() {
        return false;
    }
}
