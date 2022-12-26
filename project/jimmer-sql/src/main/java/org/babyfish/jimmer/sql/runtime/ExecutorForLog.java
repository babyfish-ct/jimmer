package org.babyfish.jimmer.sql.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

class ExecutorForLog implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorForLog.class);

    private final Executor raw;

    static Executor wrap(Executor raw) {
        if (raw == null) {
            return new ExecutorForLog(DefaultExecutor.INSTANCE);
        }
        if (raw instanceof ExecutorForLog) {
            return raw;
        }
        return new ExecutorForLog(raw);
    }

    private ExecutorForLog(Executor raw) {
        this.raw = raw;
    }

    @Override
    public <R> R execute(
            Connection con,
            String sql,
            List<Object> variables,
            ExecutionPurpose purpose,
            StatementFactory statementFactory,
            SqlFunction<PreparedStatement, R> block
    ) {
        LOGGER.info(
                "jimmer> sql: " +
                        sql +
                        ", variables: " +
                        variables +
                        ", purpose: " +
                        purpose
        );
        return raw.execute(con, sql, variables, purpose, statementFactory, block);
    }
}
