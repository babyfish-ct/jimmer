package org.babyfish.jimmer.benchmark;

import org.babyfish.jimmer.benchmark.model.Book;
import org.babyfish.jimmer.benchmark.model.BookTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class QueryBenchmark {

    @Param({"1", "10", "100"})
    private int rowCount;

    private JSqlClient sqlClient;

    private ConfigurableRootQuery<BookTable, Book> retainedQuery;

    private Connection connection;

    @Setup
    public void setup() {
        sqlClient = JSqlClient.newBuilder().build();
        retainedQuery = createQuery();
        Object[][] rows = new Object[rowCount][4];
        for (int i = 0; i < rowCount; i++) {
            rows[i] = new Object[]{
                    (long) i + 1,
                    "GraphQL in Action",
                    i + 1,
                    new BigDecimal("80.00")
            };
        }
        connection = MockJdbc.connection(rows);
    }

    @Benchmark
    public List<Book> executeOneShotQuery() {
        return createQuery().execute(connection);
    }

    @Benchmark
    public List<Book> executeRetainedQuery() {
        return retainedQuery.execute(connection);
    }

    private ConfigurableRootQuery<BookTable, Book> createQuery() {
        BookTable table = BookTable.$;
        return sqlClient
                .createQuery(table)
                .where(table.name().eq("GraphQL in Action"))
                .orderBy(table.edition())
                .select(table);
    }
}
