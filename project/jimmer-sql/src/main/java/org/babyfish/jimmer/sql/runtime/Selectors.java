package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherUtil;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Selectors {

    private static final AtomicLong CURSOR_ID_SEQUENCE = new AtomicLong();

    private Selectors() {
    }

    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose
    ) {
        return select(
                sqlClient,
                con,
                sql,
                variables,
                variablePositions,
                selections,
                tupleCreator,
                purpose,
                JdbcOptions.EMPTY
        );
    }

    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            JdbcOptions jdbcOptions
    ) {
        JdbcOptions effectiveJdbcOptions = effectiveJdbcOptions(sqlClient, jdbcOptions);
        List<R> rows = sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        sql,
                        variables,
                        variablePositions,
                        purpose,
                        null,
                        null,
                        (stmt, args) -> {
                            applyJdbcOptions(stmt, effectiveJdbcOptions);
                            Reader<?> reader = Readers.createReader(sqlClient, selections, tupleCreator);
                            return Internal.usingSqlDraftContext(draftCtx -> {
                                Reader.Context ctx = new Reader.Context(draftCtx, sqlClient);
                                try (ResultSet resultSet = stmt.executeQuery()) {
                                    return readAll(resultSet, reader, ctx);
                                }
                            });
                        }
                )
        );
        FetcherUtil.fetch(sqlClient, con, selections, tupleCreator, rows);
        return rows;
    }

    public static <R> Stream<R> stream(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            JdbcOptions jdbcOptions,
            boolean forUpdate
    ) {
        validateStreamSelections(sqlClient, selections);
        ConnectionManager connectionManager = sqlClient.getSlaveConnectionManager(forUpdate);
        return stream(
                sqlClient,
                connectionManager,
                con,
                sql,
                variables,
                variablePositions,
                selections,
                tupleCreator,
                purpose,
                jdbcOptions,
                null,
                null
        );
    }

    public static <R> Stream<R> stream(
            JSqlClientImplementor sqlClient,
            ConnectionManager connectionManager,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            JdbcOptions jdbcOptions,
            @Nullable Runnable beforeClose,
            @Nullable Consumer<Connection> connectionValidator
    ) {
        validateStreamSelections(sqlClient, selections);
        JdbcOptions effectiveJdbcOptions = effectiveJdbcOptions(sqlClient, jdbcOptions);
        Executor.Args<Void> args = new Executor.Args<>(
                sqlClient,
                null,
                sql,
                variables,
                variablePositions,
                purpose,
                null,
                null,
                (stmt, it) -> null
        );
        ConnectionManager.ConnectionScope scope = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long cursorId = CURSOR_ID_SEQUENCE.incrementAndGet();
        try {
            scope = connectionManager.open(con);
            if (connectionValidator != null) {
                connectionValidator.accept(scope.connection());
            }
            args = new Executor.Args<>(
                    sqlClient,
                    scope.connection(),
                    sql,
                    variables,
                    variablePositions,
                    purpose,
                    null,
                    null,
                    (statement, it) -> null
            );
            stmt = scope.connection().prepareStatement(sql);
            DefaultExecutor.setParameters(stmt, variables, sqlClient);
            applyJdbcOptions(stmt, effectiveJdbcOptions);
            sqlClient.getExecutor().openCursor(
                    cursorId,
                    sql,
                    variables,
                    variablePositions,
                    purpose,
                    args.ctx,
                    sqlClient
            );
            rs = stmt.executeQuery();
            Reader<?> reader = Readers.createReader(sqlClient, selections, tupleCreator);
            DraftContext draftContext = new DraftContext(null);
            Reader.Context readerContext = new Reader.Context(draftContext, sqlClient);
            return createStream(
                    sqlClient,
                    args,
                    cursorId,
                    rs,
                    stmt,
                    scope,
                    reader,
                    readerContext,
                    draftContext,
                    selections,
                    tupleCreator,
                    beforeClose
            );
        } catch (Exception ex) {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(scope);
            throw DefaultExecutor.translate(args, ex);
        }
    }

    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            boolean forUpdate) {
        return select(
                sqlClient,
                con,
                sql,
                variables,
                variablePositions,
                selections,
                tupleCreator,
                purpose,
                JdbcOptions.EMPTY,
                forUpdate
        );
    }

    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            JdbcOptions jdbcOptions,
            boolean forUpdate) {
        return sqlClient.getSlaveConnectionManager(forUpdate).execute(con, conn ->
                select(sqlClient, conn, sql, variables, variablePositions, selections, tupleCreator, purpose, jdbcOptions)
        );
    }

    @SuppressWarnings("unchecked")
    public static <R> void forEach(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            int batchSize,
            Consumer<R> consumer) {
        Executor executor = sqlClient.getExecutor();
        long cursorId = CURSOR_ID_SEQUENCE.incrementAndGet();
        Executor.Args<Void> args = new Executor.Args<>(
                sqlClient,
                con,
                sql,
                variables,
                variablePositions,
                purpose,
                null,
                (stmt, a) -> {
                    Reader<?> reader = Readers.createReader(sqlClient, selections, tupleCreator);
                    return Internal.usingSqlDraftContext((draftContext) -> {
                        Reader.Context ctx = new Reader.Context(draftContext, sqlClient);
                        List<R> results = new ArrayList<>();
                        stmt.setFetchSize(batchSize);
                        try (ResultSet resultSet = stmt.executeQuery()) {
                            while (resultSet.next()) {
                                results.add((R) reader.read(resultSet, ctx));
                                ctx.resetCol();
                                if (results.size() >= batchSize) {
                                    FetcherUtil.fetch(sqlClient, con, selections, tupleCreator, results);
                                    for (R result : results) {
                                        consumer.accept(result);
                                    }
                                    results.clear();
                                }
                            }
                        }
                        FetcherUtil.fetch(sqlClient, con, selections, tupleCreator, results);
                        for (R result : results) {
                            consumer.accept(result);
                        }
                        return null;
                    });
                },
                cursorId
        );
        executor.openCursor(cursorId, sql, variables, variablePositions, purpose, args.ctx, sqlClient);
        Long oldCursorId = Cursors.setCurrentCursorId(cursorId);
        try {
            executor.execute(args);
        } finally {
            Cursors.setCurrentCursorId(oldCursorId);
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> List<R> readAll(ResultSet resultSet, Reader<?> reader, Reader.Context ctx) throws SQLException {
        List<R> results = new ArrayList<>();
        while (resultSet.next()) {
            results.add((R) reader.read(resultSet, ctx));
            ctx.resetCol();
        }
        return results;
    }

    private static void validateStreamSelections(
            JSqlClientImplementor sqlClient,
            List<Selection<?>> selections
    ) {
        if (FetcherUtil.hasPostFetchColumns(sqlClient, selections)) {
            throw new UnsupportedOperationException(
                    "Streaming does not support fetcher selections that require post-fetch; " +
                            "use selections that can be read from the root JDBC result set"
            );
        }
    }

    private static JdbcOptions effectiveJdbcOptions(
            JSqlClientImplementor sqlClient,
            JdbcOptions jdbcOptions
    ) {
        Integer fetchSize = jdbcOptions.getFetchSize() != null ?
                jdbcOptions.getFetchSize() :
                sqlClient.getDefaultJdbcFetchSize();
        Integer queryTimeout = jdbcOptions.getQueryTimeout() != null ?
                jdbcOptions.getQueryTimeout() :
                sqlClient.getDefaultJdbcQueryTimeout();
        return JdbcOptions.of(fetchSize, queryTimeout);
    }

    private static void applyJdbcOptions(PreparedStatement stmt, JdbcOptions jdbcOptions) throws SQLException {
        if (jdbcOptions.getFetchSize() != null) {
            stmt.setFetchSize(jdbcOptions.getFetchSize());
        }
        if (jdbcOptions.getQueryTimeout() != null) {
            stmt.setQueryTimeout(jdbcOptions.getQueryTimeout());
        }
    }

    private static <R> Stream<R> createStream(
            JSqlClientImplementor sqlClient,
            Executor.Args<?> args,
            long cursorId,
            ResultSet resultSet,
            PreparedStatement stmt,
            ConnectionManager.ConnectionScope scope,
            Reader<?> reader,
            Reader.Context readerContext,
            DraftContext draftContext,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            @Nullable Runnable beforeClose
    ) {
        JdbcResourceCloser closer = new JdbcResourceCloser(
                sqlClient,
                cursorId,
                resultSet,
                stmt,
                scope,
                draftContext,
                beforeClose
        );
        Iterator<R> iterator = new Iterator<R>() {

            private boolean loaded;

            private boolean hasNext;

            @Override
            public boolean hasNext() {
                if (!loaded) {
                    try {
                        hasNext = resultSet.next();
                        loaded = true;
                        if (!hasNext) {
                            closer.close();
                        }
                    } catch (SQLException ex) {
                        closer.closeSilently(false);
                        throw DefaultExecutor.translate(args, ex);
                    }
                }
                return hasNext;
            }

            @Override
            @SuppressWarnings("unchecked")
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                loaded = false;
                try {
                    Object row = reader.read(resultSet, readerContext);
                    readerContext.resetCol();
                    return (R) FetcherUtil.convert(selections, tupleCreator, row);
                } catch (SQLException ex) {
                    closer.closeSilently(false);
                    throw DefaultExecutor.translate(args, ex);
                } catch (RuntimeException ex) {
                    closer.closeSilently(false);
                    throw ex;
                }
            }
        };
        Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false).onClose(closer::close);
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static class JdbcResourceCloser {

        private final JSqlClientImplementor sqlClient;

        private final long cursorId;

        private final ResultSet resultSet;

        private final PreparedStatement stmt;

        private final ConnectionManager.ConnectionScope scope;

        private final DraftContext draftContext;

        @Nullable
        private final Runnable beforeClose;

        private boolean closed;

        private JdbcResourceCloser(
                JSqlClientImplementor sqlClient,
                long cursorId,
                ResultSet resultSet,
                PreparedStatement stmt,
                ConnectionManager.ConnectionScope scope,
                DraftContext draftContext,
                @Nullable Runnable beforeClose
        ) {
            this.sqlClient = sqlClient;
            this.cursorId = cursorId;
            this.resultSet = resultSet;
            this.stmt = stmt;
            this.scope = scope;
            this.draftContext = draftContext;
            this.beforeClose = beforeClose;
        }

        void close() {
            close(true);
        }

        void close(boolean runBeforeClose) {
            if (closed) {
                return;
            }
            closed = true;
            RuntimeException runtimeException = null;
            try {
                resultSet.close();
            } catch (SQLException ex) {
                runtimeException = new RuntimeException(ex);
            }
            try {
                stmt.close();
            } catch (SQLException ex) {
                if (runtimeException == null) {
                    runtimeException = new RuntimeException(ex);
                } else {
                    runtimeException.addSuppressed(ex);
                }
            }
            if (runBeforeClose && beforeClose != null) {
                try {
                    beforeClose.run();
                } catch (RuntimeException ex) {
                    if (runtimeException == null) {
                        runtimeException = ex;
                    } else {
                        runtimeException.addSuppressed(ex);
                    }
                }
            }
            try {
                scope.close();
            } catch (RuntimeException ex) {
                if (runtimeException == null) {
                    runtimeException = ex;
                } else {
                    runtimeException.addSuppressed(ex);
                }
            } finally {
                draftContext.dispose();
                sqlClient.getExecutor().closeCursor(cursorId);
            }
            if (runtimeException != null) {
                throw runtimeException;
            }
        }

        void closeSilently(boolean runBeforeClose) {
            try {
                close(runBeforeClose);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static <R> void forEach(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            TupleCreator<?> tupleCreator,
            ExecutionPurpose purpose,
            int batchSize,
            Consumer<R> consumer,
            boolean forUpdate) {
        sqlClient.getSlaveConnectionManager(forUpdate).execute(con, conn -> {
                    forEach(sqlClient, conn, sql, variables, variablePositions, selections,
                            tupleCreator, purpose, batchSize, consumer);
                    return null;
                }
        );
    }
}
