package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public <R> R execute(@NotNull Args<R> args) {
        if (!LOGGER.isInfoEnabled()) {
            return raw.execute(args);
        }
        if (args.sqlClient.getSqlFormatter().isPretty()) {
            return prettyLog(args, args.sqlClient.getSqlFormatter().getMaxVariableContentLength());
        }
        return simpleLog(args);
    }

    private <R> R simpleLog(Args<R> args) {
        ExecutorContext ctx = args.ctx;
        String sql = args.sql;
        List<Object> variables = args.variables;
        if (ctx == null) {
            LOGGER.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            args.purpose
            );
        } else {
            Logger logger = LoggerFactory.getLogger(ctx.getPrimaryElement().getClassName());
            logger.info(
                    "jimmer> sql: " +
                            sql +
                            ", variables: " +
                            variables +
                            ", purpose: " +
                            args.purpose
            );
            for (StackTraceElement element : ctx.getMatchedElements()) {
                logger.info(
                        "jimmer stacktrace-element)> {}",
                        element
                );
            }
        }
        return raw.execute(args);
    }

    private <R> R prettyLog(Args<R> args, int maxVariableContentLength) {
        R result = null;
        Throwable throwable = null;
        long millis = System.currentTimeMillis();
        try {
            result = raw.execute(args);
        } catch (RuntimeException | Error ex) {
            throwable = ex;
        }
        millis = System.currentTimeMillis() - millis;
        int affectedRowCount = -1;
        char ch = args.sql.charAt(0);
        if ((ch == 'i' || ch == 'u' || ch == 'd') && result instanceof Integer) {
            affectedRowCount = (Integer)result;
        }
        prettyPrint(
                args.sql,
                args.variables,
                args.variableIndices,
                args.purpose,
                affectedRowCount,
                throwable,
                millis,
                args.ctx,
                maxVariableContentLength
        );
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }
        if (throwable != null) {
            throw (Error)throwable;
        }
        return result;
    }

    private void prettyPrint(
            String sql,
            List<Object> variables,
            List<Integer> variableIndices,
            ExecutionPurpose purpose,
            int affectedRowCount,
            Throwable throwable,
            long millis,
            ExecutorContext ctx,
            int maxVariableContentLength
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("===>SQL: \n");
        if (variableIndices == null) {
            builder.append(sql);
        } else {
            int cloneFrom = 0;
            int paramIndex = 0;
            for (int index : variableIndices) {
                builder.append(sql, cloneFrom, index);
                cloneFrom = index;
                builder.append(" /* ");
                appendEmbeddedVariable(builder, variables.get(paramIndex++), maxVariableContentLength);
                builder.append(" */");
            }
            int len = sql.length();
            if (cloneFrom < len) {
                builder.append(sql, cloneFrom, len);
            }
        }
        builder.append('\n');

        builder.append("Purpose: ").append(purpose).append('\n');

        if (affectedRowCount != -1) {
            builder.append("Affected row count: ").append(affectedRowCount).append('\n');
        }
        if (throwable == null) {
            builder.append("JDBC response status: success\n");
        } else {
            builder.append("JDBC response status: failed<").append(throwable.getClass().getName()).append(">\n");
        }
        builder.append("Time cost: ").append(millis).append("ms\n");

        if (ctx != null) {
            builder.append("--- Business related stack trace information ---\n");
            for (StackTraceElement element : ctx.getMatchedElements()) {
                builder.append(element).append('\n');
            }
        }
        builder.append("<===");
        LOGGER.info(builder.toString());
    }

    private void appendEmbeddedVariable(
            StringBuilder builder,
            Object variable,
            int maxVariableContentLength
    ) {
        if (maxVariableContentLength < 10) {
            maxVariableContentLength = 10;
        }
        if (variable instanceof DbNull) {
            builder.append("<null: ").append(((DbNull)variable).getType().getSimpleName()).append('>');
        } else if (variable instanceof String) {
            String text = (String) variable;
            if (text.length() > maxVariableContentLength) {
                builder.append(text, 0, maxVariableContentLength - 3).append("...");
            } else {
                builder.append(text);
            }
        } else if (variable instanceof byte[]) {
            byte[] arr = (byte[]) variable;
            int len = arr.length > maxVariableContentLength ? maxVariableContentLength - 3 : arr.length;
            builder.append("bytes[");
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(arr[i]);
            }
            if (arr.length > maxVariableContentLength) {
                builder.append(", ...");
            }
            builder.append(']');
        } else {
            builder.append(variable);
        }
    }
}
