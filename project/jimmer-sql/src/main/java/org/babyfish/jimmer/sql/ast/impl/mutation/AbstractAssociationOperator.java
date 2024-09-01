package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.runtime.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class AbstractAssociationOperator {

    final JSqlClientImplementor sqlClient;

    final Connection con;

    AbstractAssociationOperator(JSqlClientImplementor sqlClient, Connection con) {
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

    final int execute(SqlBuilder builder) {
        return execute(builder, PreparedStatement::executeUpdate);
    }

    final int execute(
            BatchSqlBuilder builder,
            Collection<?> rows,
            BiFunction<SQLException, Executor.BatchContext, Exception> exceptionTranslator
    ) {
        int[] rowCounts = executeImpl(builder, rows, exceptionTranslator);
        return sumRowCount(rowCounts);
    }

    final int sumRowCount(int[] rowCounts) {
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

    int[] executeImpl(
            BatchSqlBuilder builder,
            Collection<?> rows,
            BiFunction<SQLException, Executor.BatchContext, Exception> exceptionTranslator
    ) {
        Tuple2<String, BatchSqlBuilder.VariableMapper> sqlTuple = builder.build();
        try (Executor.BatchContext batchContext = sqlClient
                .getExecutor().executeBatch(
                        con,
                        sqlTuple.get_1(),
                        null,
                        ExecutionPurpose.command(QueryReason.NONE),
                        sqlClient
                )
        ) {
            BatchSqlBuilder.VariableMapper mapper = sqlTuple.get_2();
            for (Object row : rows) {
                batchContext.add(mapper.variables(row));
            }
            return batchContext.execute(exceptionTranslator);
        }
    }

    boolean isBatchStatementSimple() {
        return false;
    }

    static List<ChildTableOperator> createSubOperators(
            JSqlClientImplementor sqlClient,
            MutationPath path,
            DisconnectingType disconnectingType,
            Function<ImmutableProp, ChildTableOperator> backPropCreator
    ) {
        List<ChildTableOperator> subOperators = null;
        if (path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backProp : sqlClient.getEntityManager().getAllBackProps(path.getType())) {
                if (backProp.isColumnDefinition() && disconnectingType != DisconnectingType.NONE) {
                    if (subOperators == null) {
                        subOperators = new ArrayList<>();
                    }
                    ChildTableOperator subOperator = backPropCreator.apply(backProp);
                    if (subOperator.disconnectingType != DisconnectingType.NONE) {
                        subOperators.add(subOperator);
                    }
                }
            }
        }
        if (subOperators == null) {
            return Collections.emptyList();
        }
        return subOperators;
    }

    static List<MiddleTableOperator> createMiddleTableOperators(
            JSqlClientImplementor sqlClient,
            MutationPath path,
            DisconnectingType disconnectingType,
            Function<ImmutableProp, MiddleTableOperator> propCreator,
            Function<ImmutableProp, MiddleTableOperator> backPropCreator
    ) {
        List<MiddleTableOperator> middleTableOperators = null;
        for (ImmutableProp prop : path.getType().getProps().values()) {
            if (prop.isMiddleTableDefinition()) {
                if (middleTableOperators == null) {
                    middleTableOperators = new ArrayList<>();
                }
                MiddleTableOperator middleTableOperator = propCreator.apply(prop);
                if (!middleTableOperator.middleTable.isReadonly()) {
                    middleTableOperators.add(middleTableOperator);
                }
            }
        }
        if (path.getParent() == null || disconnectingType.isDelete()) {
            for (ImmutableProp backProp : sqlClient.getEntityManager().getAllBackProps(path.getType())) {
                if (backProp.isMiddleTableDefinition()) {
                    if (middleTableOperators == null) {
                        middleTableOperators = new ArrayList<>();
                    }
                    MiddleTableOperator middleTableOperator = backPropCreator.apply(backProp);
                    if (!middleTableOperator.middleTable.isReadonly()) {
                        middleTableOperators.add(middleTableOperator);
                    }
                }
            }
        }
        if (middleTableOperators == null) {
            return Collections.emptyList();
        }
        return middleTableOperators;
    }
}
