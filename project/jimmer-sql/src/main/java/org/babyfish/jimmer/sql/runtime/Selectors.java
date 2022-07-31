package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.fetcher.impl.Fetchers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Selectors {

    private Selectors() {}

    @SuppressWarnings("unchecked")
    public static <R> List<R> select(
            JSqlClient sqlClient,
            Connection con,
            String sql,
            List<Object> variables,
            List<Selection<?>> selections
    ) {
        return sqlClient.getExecutor().execute(con, sql, variables, stmt -> {
            List<R> results = new ArrayList<>();
            try (ResultSet resultSet = stmt.executeQuery()) {
                ResultMapper resultMapper = new ResultMapper(sqlClient, selections, resultSet);
                while (resultSet.next()) {
                    results.add((R)resultMapper.map());
                }
            }
            Fetchers.fetch(sqlClient, con, selections, results);
            return results;
        });
    }
}
