package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Executor {

    <R> R execute(@NotNull Args<R> args);

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
        return ExecutorForLog.wrap(DefaultExecutor.INSTANCE);
    }

    static Executor log(Executor executor) {
        return ExecutorForLog.wrap(executor);
    }

    class Args<R> {

        public final JSqlClientImplementor sqlClient;

        public final Connection con;

        public final String sql;

        public final List<Object> variables;

        @Nullable
        public final List<Integer> variablePositions;

        public final ExecutionPurpose purpose;

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
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block
        ) {
            this.sqlClient = sqlClient;
            this.con = con;
            this.sql = sql;
            this.variables = variables;
            this.variablePositions = variablePositions;
            this.purpose = purpose;
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
            this.ctx = ExecutorContext.create(sqlClient);
            this.statementFactory = statementFactory;
            this.block = block;
            this.closingCursorId = closingCursorId;
        }
    }
}
