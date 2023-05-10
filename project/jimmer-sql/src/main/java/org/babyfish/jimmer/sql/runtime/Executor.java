package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public interface Executor {

    <R> R execute(@NotNull Args<R> args);

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
        public final List<Integer> variableIndices;

        public final ExecutionPurpose purpose;

        @Nullable
        public final ExecutorContext ctx;

        public final StatementFactory statementFactory;

        public final SqlFunction<PreparedStatement, R> block;

        public Args(
                JSqlClientImplementor sqlClient,
                Connection con,
                String sql,
                List<Object> variables,
                List<Integer> variableIndices,
                ExecutionPurpose purpose,
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block
        ) {
            this.sqlClient = sqlClient;
            this.con = con;
            this.sql = sql;
            this.variables = variables;
            this.variableIndices = variableIndices;
            this.purpose = purpose;
            this.ctx = ExecutorContext.create(sqlClient);
            this.statementFactory = statementFactory;
            this.block = block;
        }
    }
}
