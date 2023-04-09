package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;

public interface Executor {

    <R> R execute(
            Connection con,
            String sql,
            List<Object> variables,
            ExecutionPurpose purpose,
            @Nullable ExecutorContext ctx,
            StatementFactory statementFactory,
            SqlFunction<PreparedStatement, R> block
    );

    static Executor log() {
        return ExecutorForLog.wrap(DefaultExecutor.INSTANCE);
    }

    static Executor log(Executor executor) {
        return ExecutorForLog.wrap(executor);
    }
}
