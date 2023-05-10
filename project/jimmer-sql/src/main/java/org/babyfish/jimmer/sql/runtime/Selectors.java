package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.impl.Fetchers;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Selectors {

    private static final AtomicLong CURSOR_ID_SEQUENCE = new AtomicLong();

    private Selectors() {}

    @SuppressWarnings("unchecked")
    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            ExecutionPurpose purpose
    ) {
        List<R> rows = sqlClient.getExecutor().execute(
                new Executor.Args<>(
                        sqlClient,
                        con,
                        sql,
                        variables,
                        variablePositions,
                        purpose,
                        null,
                        stmt -> {
                            Reader<?> reader = Readers.createReader(sqlClient, selections);
                            Reader.Col col = new Reader.Col();
                            List<R> results = new ArrayList<>();
                            try (ResultSet resultSet = stmt.executeQuery()) {
                                while (resultSet.next()) {
                                    results.add((R)reader.read(resultSet, col));
                                    col.reset();
                                }
                            }
                            return results;
                        }
                )
        );
        Fetchers.fetch(sqlClient, con, selections, rows);
        return rows;
    }

    @SuppressWarnings("unchecked")
    public static <R> void forEach(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            @Nullable List<Integer> variablePositions,
            List<Selection<?>> selections,
            ExecutionPurpose purpose,
            int batchSize,
            Consumer<R> consumer
    ) {
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
                stmt -> {
                    Reader<?> reader = Readers.createReader(sqlClient, selections);
                    Reader.Col col = new Reader.Col();
                    List<R> results = new ArrayList<>();
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        while (resultSet.next()) {
                            results.add((R) reader.read(resultSet, col));
                            col.reset();
                            if (results.size() >= batchSize) {
                                Fetchers.fetch(sqlClient, con, selections, results);
                                for (R result : results) {
                                    consumer.accept(result);
                                }
                                results.clear();
                            }
                        }
                    }
                    Fetchers.fetch(sqlClient, con, selections, results);
                    for (R result : results) {
                        consumer.accept(result);
                    }
                    return null;
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
}
