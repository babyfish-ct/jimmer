package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.impl.Fetchers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Selectors {

    private Selectors() {}

    @SuppressWarnings("unchecked")
    public static <R> List<R> select(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            List<Selection<?>> selections,
            ExecutionPurpose purpose
    ) {
        return sqlClient.getExecutor().execute(con, sql, variables, purpose, ExecutorContext.create(sqlClient), null, stmt -> {
            Reader<?> reader = Readers.createReader(sqlClient, selections);
            Reader.Col col = new Reader.Col();
            List<R> results = new ArrayList<>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    results.add((R)reader.read(resultSet, col));
                    col.reset();
                }
            }
            Fetchers.fetch(sqlClient, con, selections, results);
            return results;
        });
    }

    @SuppressWarnings("unchecked")
    public static <R> void forEach(
            JSqlClientImplementor sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            List<Selection<?>> selections,
            ExecutionPurpose purpose,
            int batchSize,
            Consumer<R> consumer
    ) {
        sqlClient.getExecutor().execute(con, sql, variables, purpose, ExecutorContext.create(sqlClient), null, stmt -> {
            Reader<?> reader = Readers.createReader(sqlClient, selections);
            Reader.Col col = new Reader.Col();
            List<R> results = new ArrayList<>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    results.add((R)reader.read(resultSet, col));
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
            return (Void) null;
        });
    }
}
