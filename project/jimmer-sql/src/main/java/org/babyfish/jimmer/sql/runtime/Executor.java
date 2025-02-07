package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

public interface Executor {

    <R> R execute(@NotNull Args<R> args);

    BatchContext executeBatch(
            @NotNull Connection con,
            @NotNull String sql,
            @Nullable ImmutableProp generatedIdProp,
            @NotNull ExecutionPurpose purpose,
            @NotNull JSqlClientImplementor sqlClient
    );

    /**
     * This method will never be invoked unless the current operation is `Query.forEach`
     *
     * <p>For `Query.forEach`, SQL execution result log have to be printed after children fetching,
     * this method can give SQL logger a chance to print SQL before children fetching</p>
     */
    default void openCursor(
            long cursorId,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            ExecutionPurpose purpose,
            @Nullable ExecutorContext ctx,
            JSqlClientImplementor sqlClient
    ) {}

    static Executor log() {
        return ExecutorForLog.wrap(DefaultExecutor.INSTANCE, null);
    }

    static Executor log(Executor executor) {
        return ExecutorForLog.wrap(executor, null);
    }

    static Executor log(Logger logger) {
        return ExecutorForLog.wrap(DefaultExecutor.INSTANCE, logger);
    }

    static Executor log(Executor executor, Logger logger) {
        return ExecutorForLog.wrap(executor, logger);
    }

    static void validateMutationConnection(Connection con) {
        try {
            if (con.getAutoCommit()) {
                throw new ExecutionException(
                        "When configuration about mutation-transaction-required" +
                                "(Either global configuration or mutation operation level configuration) is enabled, " +
                                "the mutation operation must be executed " +
                                "based on JDBC connection without auto commit, " +
                                "Do you forget to open transaction?"
                );
            }
        } catch (SQLException e) {
            throw new ExecutionException(
                    "Failed to retrieve the auto-commit mode of the connection"
            );
        }
    }

    class Args<R> implements ExceptionTranslator.Args {

        public final JSqlClientImplementor sqlClient;

        public final Connection con;

        public final String sql;

        public final List<Object> variables;

        @Nullable
        public final List<Integer> variablePositions;

        public final ExecutionPurpose purpose;

        private final ExceptionTranslator<?> exceptionTranslator;

        @Nullable
        public final ExecutorContext ctx;

        public final StatementFactory statementFactory;

        public final SqlFunction<PreparedStatement, R> block;

        /**
         * If non-null, it means the current callback is not normal operation,
         * but `Query.forEach`
         */
        @Nullable
        public final Long closingCursorId;

        public Args(
                JSqlClientImplementor sqlClient,
                Connection con,
                String sql,
                List<Object> variables,
                @Nullable List<Integer> variablePositions,
                ExecutionPurpose purpose,
                ExceptionTranslator<?> exceptionTranslator,
                @Nullable
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block
        ) {
            this.sqlClient = sqlClient;
            this.con = con;
            this.sql = sql;
            this.variables = variables;
            this.variablePositions = variablePositions;
            this.purpose = purpose;
            this.exceptionTranslator = exceptionTranslator != null ?
                    exceptionTranslator :
                    sqlClient.getExceptionTranslator();
            this.ctx = ExecutorContext.create(sqlClient);
            this.statementFactory = statementFactory;
            this.block = block;
            this.closingCursorId = null;
        }

        public Args(
                JSqlClientImplementor sqlClient,
                Connection con,
                String sql,
                List<Object> variables,
                @Nullable List<Integer> variablePositions,
                ExecutionPurpose purpose,
                @Nullable
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block,
                long closingCursorId
        ) {
            this.sqlClient = sqlClient;
            this.con = con;
            this.sql = sql;
            this.variables = variables;
            this.variablePositions = variablePositions;
            this.purpose = purpose;
            this.exceptionTranslator = null;
            this.ctx = ExecutorContext.create(sqlClient);
            this.statementFactory = statementFactory;
            this.block = block;
            this.closingCursorId = closingCursorId;
        }

        @Override
        public JSqlClientImplementor sqlClient() {
            return sqlClient;
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public ExecutionPurpose purpose() {
            return purpose;
        }

        public ExceptionTranslator<?> getExceptionTranslator() {
            return exceptionTranslator;
        }

        @Override
        public ExecutorContext ctx() {
            return ctx;
        }
    }

    interface BatchContext extends ExceptionTranslator.Args, AutoCloseable {
        JSqlClientImplementor sqlClient();
        String sql();
        ExecutionPurpose purpose();
        ExecutorContext ctx();
        void add(List<Object> variables);
        int[] execute(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator);
        Object[] generatedIds();
        void addExecutedListener(Runnable listener);

        @Override
        void close();
    }
}
