package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

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
            @Nullable ExecutorContext ctx,
            StatementFactory statementFactory,
            SqlFunction<PreparedStatement, R> block
    ) {
        if (ctx == null) {
            LOGGER.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            purpose
            );
        } else {
            Logger logger = LoggerFactory.getLogger(ctx.getPrimaryElement().getClassName());
            LOGGER.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            purpose
            );
            for (StackTraceElement element : ctx.getMatchedElements()) {
                LOGGER.info(
                        "jimmer stacktrace-element)> {}",
                        element
                );
            }
        }
        return raw.execute(con, sql, variables, purpose, ctx, statementFactory, block);
    }
}
