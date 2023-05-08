package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.dialect.Dialect;
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

        public final Connection con;

        public final String sql;

        public final List<Object> variables;

        public final ExecutionPurpose purpose;

        @Nullable
        public final ExecutorContext ctx;

        public final StatementFactory statementFactory;

        public final Dialect dialect;

        public final SqlFunction<PreparedStatement, R> block;

        public Args(
                Connection con,
                String sql,
                List<Object> variables,
                ExecutionPurpose purpose,
                @Nullable ExecutorContext ctx,
                StatementFactory statementFactory,
                Dialect dialect,
                SqlFunction<PreparedStatement, R> block
        ) {
            this.con = con;
            this.sql = sql;
            this.variables = variables;
            this.purpose = purpose;
            this.ctx = ctx;
            this.statementFactory = statementFactory;
            this.dialect = dialect;
            this.block = block;
        }
    }
}
