package org.babyfish.jimmer.sql.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;

public class ExecutorForLog extends AbstractExecutorProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorForLog.class);

    private static final String REQUEST = "===>";

    private static final String RESPONSE = "<===";
    
    private final Logger logger;

    public static Executor wrap(Executor raw, Logger logger) {
        return applier(
                ExecutorForLog.class,
                p -> p.logger == logger,
                r -> new ExecutorForLog(r, logger)
        ).applyTo(raw);
    }

    private ExecutorForLog(Executor raw, Logger logger) {
        super(raw);
        this.logger = logger != null ? logger : LOGGER;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        if (!logger.isInfoEnabled()) {
            return raw.execute(args);
        }
        if (args.sqlClient.getSqlFormatter().isPretty()) {
            return prettyLog(args);
        }
        return simpleLog(args);
    }

    @Override
    public void openCursor(
            long cursorId,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            ExecutionPurpose purpose,
            @Nullable ExecutorContext ctx,
            JSqlClientImplementor sqlClient
    ) {
        if (!logger.isInfoEnabled()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Open cursor(").append(cursorId).append(')').append(REQUEST).append('\n');
        appendPrettyRequest(
                builder,
                sql,
                variables,
                variablePositions,
                purpose,
                ctx,
                sqlClient
        );
        logger.info(builder.toString());
    }

    @Override
    protected AbstractExecutorProxy recreate(Executor raw) {
        return new ExecutorForLog(raw, logger);
    }

    @Override
    protected Batch createBatch(BatchContext raw) {
        return new Batch(raw, logger);
    }

    private <R> R simpleLog(Args<R> args) {
        ExecutorContext ctx = args.ctx;
        String sql = args.sql;
        List<Object> variables = args.variables;
        if (ctx == null) {
            logger.info(
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

    private <R> R prettyLog(Args<R> args) {
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

        StringBuilder builder = new StringBuilder();
        if (args.closingCursorId == null) {
            builder.append("Execute SQL").append(REQUEST).append('\n');
            appendPrettyRequest(
                    builder,
                    args.sql,
                    args.variables,
                    args.variablePositions,
                    args.purpose,
                    args.ctx,
                    args.sqlClient
            );
        }
        appendPrettyResponse(
                builder,
                affectedRowCount,
                throwable,
                millis
        );
        if (args.closingCursorId != null) {
            builder.append(RESPONSE).append("Close cursor(").append(args.closingCursorId).append(')');
        } else {
            Long currentCourseId = Cursors.currentCursorId();
            if (currentCourseId != null) {
                builder.append("CursorId: ").append(currentCourseId).append('\n');
            }
            builder.append(RESPONSE).append("Execute SQL");
        }

        logger.info(builder.toString());

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }
        if (throwable != null) {
            throw (Error)throwable;
        }
        return result;
    }

    private static void appendPrettyRequest(
            StringBuilder builder,
            String sql,
            List<Object> variables,
            List<Integer> variablePositions,
            ExecutionPurpose purpose,
            ExecutorContext ctx,
            JSqlClientImplementor sqlClient
    ) {
        if (ctx != null) {
            builder.append("--- Business related stack trace information ---\n");
            for (StackTraceElement element : ctx.getMatchedElements()) {
                builder.append(element).append('\n');
            }
        }

        builder.append("Purpose: ").append(purpose).append('\n');

        builder.append("SQL: ");
        if (variablePositions == null) {
            builder.append(sql);
        } else {
            sqlClient.getSqlFormatter().append(
                    builder,
                    sql,
                    variables,
                    variablePositions
            );
        }
        builder.append('\n');
    }

    private static void appendPrettyResponse(
            StringBuilder builder,
            int affectedRowCount,
            Throwable throwable,
            long millis
    ) {
        if (affectedRowCount != -1) {
            builder.append("Affected row count: ").append(affectedRowCount).append('\n');
        }
        if (throwable == null) {
            builder.append("JDBC response status: success\n");
        } else {
            builder.append("JDBC response status: failed<").append(throwable.getClass().getName()).append(">\n");
        }
        builder.append("Time cost: ").append(millis).append("ms\n");
    }

    protected static class Batch extends AbstractExecutorProxy.Batch {

        private final Logger logger;

        private List<List<Object>> variableMatrix = new ArrayList<>();

        Batch(BatchContext raw, Logger logger) {
            super(raw);
            this.logger = logger;
        }

        @Override
        public JSqlClientImplementor sqlClient() {
            return raw.sqlClient();
        }

        @Override
        public String sql() {
            return raw.sql();
        }

        @Override
        public ExecutionPurpose purpose() {
            return raw.purpose();
        }

        @Override
        public ExecutorContext ctx() {
            return raw.ctx();
        }

        @Override
        public void add(List<Object> variables) {
            raw.add(variables);
            variableMatrix.add(variables);
        }

        @Override
        public int[] execute(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator) {
            if (!logger.isInfoEnabled()) {
                return raw.execute(exceptionTranslator);
            }
            if (raw.sqlClient().getSqlFormatter().isPretty()) {
                return prettyLog(exceptionTranslator);
            }
            return simpleLog(exceptionTranslator);
        }

        @Override
        public Object[] generatedIds() {
            return raw.generatedIds();
        }

        @Override
        public void addExecutedListener(Runnable listener) {
            raw.addExecutedListener(listener);
        }

        @Override
        public void close() {
            raw.close();
        }

        private int[] simpleLog(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator) {
            ExecutorContext ectx = raw.ctx();
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            int size = variableMatrix.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append("batch-").append(i).append(": ");
                builder.append(variableMatrix.get(i));
            }
            builder.append("}");
            if (ectx == null) {
                logger.info(
                        "jimmer> sql: " +
                                raw.sql() +
                                ", variables: " +
                                builder +
                                ", purpose: " +
                                raw.purpose()
                );
            } else {
                Logger logger = LoggerFactory.getLogger(ectx.getPrimaryElement().getClassName());
                logger.info(
                        "jimmer> sql: " +
                                raw.sql() +
                                ", variables: " +
                                builder +
                                ", purpose: " +
                                raw.purpose()
                );
                for (StackTraceElement element : ectx.getMatchedElements()) {
                    logger.info(
                            "jimmer stacktrace-element)> {}",
                            element
                    );
                }
            }
            return raw.execute(exceptionTranslator);
        }

        private int[] prettyLog(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator) {
            int[] rowCounts = null;
            Throwable throwable = null;
            long millis = System.currentTimeMillis();
            try {
                rowCounts = raw.execute(exceptionTranslator);
            } catch (RuntimeException | Error ex) {
                throwable = ex;
            }
            millis = System.currentTimeMillis() - millis;
            int affectedRowCount = -1;
            char ch = raw.sql().charAt(0);
            if ((ch == 'i' || ch == 'u' || ch == 'd') && rowCounts != null) {
                affectedRowCount = 0;
                for (int rowCount : rowCounts) {
                    affectedRowCount += rowCount;
                }
            }

            StringBuilder builder = new StringBuilder();
            builder.append("Execute SQL").append(REQUEST).append('\n');
            appendPrettyRequest(
                    builder,
                    raw.sql(),
                    Collections.emptyList(),
                    null,
                    raw.purpose(),
                    raw.ctx(),
                    raw.sqlClient()
            );
            int size = variableMatrix.size();
            for (int i = 0; i < size; i++) {
                builder.append("batch-").append(i).append(": ");
                builder.append(variableMatrix.get(i)).append('\n');
            }
            appendPrettyResponse(
                    builder,
                    affectedRowCount,
                    throwable,
                    millis
            );
            logger.info(builder.toString());

            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
            return rowCounts;
        }
    }
}
