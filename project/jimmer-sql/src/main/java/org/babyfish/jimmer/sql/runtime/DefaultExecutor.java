package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.collection.TypedList;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

public class DefaultExecutor implements Executor {

    public static final DefaultExecutor INSTANCE = new DefaultExecutor();

    DefaultExecutor() {}

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        String sql = args.sql;
        List<Object> variables = args.variables;
        JSqlClientImplementor sqlClient = args.sqlClient;
        try (PreparedStatement stmt = args.statementFactory != null ?
                args.statementFactory.preparedStatement(args.con, sql) :
                args.con.prepareStatement(sql)
        ) {
            setParameters(stmt, variables, sqlClient);
            return args.block.apply(stmt);
        } catch (Exception ex) {
            throw new ExecutionException(
                    "Cannot execute SQL statement: " +
                            sql +
                            ", variables: " +
                            variables,
                    ex
            );
        }
    }

    @Override
    public BatchContext executeBatch(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            StatementFactory statementFactory
    ) {
        PreparedStatement stmt;
        try {
            if (statementFactory != null) {
                stmt = statementFactory.preparedStatement(con, sql);
            } else {
                stmt = con.prepareStatement(sql);
            }
        } catch (SQLException ex) {
            throw new ExecutionException(
                    "Cannot create the batch SQL statement: " + sql,
                    ex
            );
        }
        return new BatchContextImpl(sql, stmt, sqlClient);
    }

    private static void setParameters(
            PreparedStatement stmt,
            List<Object> variables,
            JSqlClientImplementor sqlClient
    ) throws Exception {
        ParameterIndex parameterIndex = new ParameterIndex();
        for (Object variable : variables) {
            if (variable instanceof DbLiteral) {
                DbLiteral literal = (DbLiteral) variable;
                literal.setParameter(
                        stmt,
                        parameterIndex,
                        sqlClient
                );
            } else if (variable instanceof TypedList<?>) {
                TypedList<?> typedList = (TypedList<?>) variable;
                stmt.setArray(
                        parameterIndex.get(),
                        stmt.getConnection().createArrayOf(typedList.getSqlElementType(), typedList.toArray())
                );
            } else {
                stmt.setObject(parameterIndex.get(), variable);
            }
        }
    }

    private static class BatchContextImpl implements BatchContext {

        private final String sql;

        private final PreparedStatement statement;

        private final JSqlClientImplementor sqlClient;

        private BatchContextImpl(
                String sql,
                PreparedStatement statement,
                JSqlClientImplementor sqlClient
        ) {
            this.sql = sql;
            this.statement = statement;
            this.sqlClient = sqlClient;
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public void add(List<Object> variables) {
            try {
                setParameters(statement, variables, sqlClient);
                statement.addBatch();
            } catch (Exception ex) {
                throw new ExecutionException(
                        "Cannot add batch into the batch SQL statement: " +
                                sql +
                                ", variables: " +
                                variables,
                        ex
                );
            }
        }

        @Override
        public int[] execute() {
            try {
                return statement.executeBatch();
            } catch (SQLException ex) {
                throw new ExecutionException(
                        "Cannot execute the batch SQL statement: " + sql,
                        ex
                );
            }
        }

        @Override
        public void close() {
            try {
                statement.close();
            } catch (SQLException ex) {
                throw new ExecutionException(
                        "Cannot execute the batch SQL statement: " + sql,
                        ex
                );
            }
        }
    }
}
