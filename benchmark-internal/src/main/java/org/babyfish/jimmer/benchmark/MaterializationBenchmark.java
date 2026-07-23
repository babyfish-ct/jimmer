package org.babyfish.jimmer.benchmark;

import org.babyfish.jimmer.benchmark.model.Book;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.Reader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MaterializationBenchmark {

    private MaterializationCase oneRow;

    private MaterializationCase oneHundredRows;

    private MaterializationCase oneThousandRows;

    private MaterializationCase tenThousandRows;

    @Setup
    public void setup() throws SQLException {
        JSqlClientImplementor sqlClient =
                (JSqlClientImplementor) JSqlClient.newBuilder().build();
        Reader<?> reader = sqlClient.getReader(ImmutableType.get(Book.class));
        oneRow = new MaterializationCase(sqlClient, reader, 1);
        oneHundredRows = new MaterializationCase(sqlClient, reader, 100);
        oneThousandRows = new MaterializationCase(sqlClient, reader, 1_000);
        tenThousandRows = new MaterializationCase(sqlClient, reader, 10_000);
    }

    @Benchmark
    @OperationsPerInvocation(1)
    public void readOneRawRow(Blackhole blackhole) throws SQLException {
        oneRow.readRaw(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(1)
    public void readOneJimmerRow(Blackhole blackhole) throws SQLException {
        oneRow.readJimmer(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void readOneHundredRawRows(Blackhole blackhole) throws SQLException {
        oneHundredRows.readRaw(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(100)
    public void readOneHundredJimmerRows(Blackhole blackhole) throws SQLException {
        oneHundredRows.readJimmer(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(1_000)
    public void readOneThousandRawRows(Blackhole blackhole) throws SQLException {
        oneThousandRows.readRaw(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(1_000)
    public void readOneThousandJimmerRows(Blackhole blackhole) throws SQLException {
        oneThousandRows.readJimmer(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(10_000)
    public void readTenThousandRawRows(Blackhole blackhole) throws SQLException {
        tenThousandRows.readRaw(blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(10_000)
    public void readTenThousandJimmerRows(Blackhole blackhole) throws SQLException {
        tenThousandRows.readJimmer(blackhole);
    }

    private static class MaterializationCase {

        private final JSqlClientImplementor sqlClient;

        private final Reader<?> reader;

        private final PreparedStatement statement;

        private MaterializationCase(
                JSqlClientImplementor sqlClient,
                Reader<?> reader,
                int rowCount
        ) throws SQLException {
            this.sqlClient = sqlClient;
            this.reader = reader;
            Object[][] rows = new Object[rowCount][];
            String name = "GraphQL in Action";
            BigDecimal price = new BigDecimal("80.00");
            for (int i = 0; i < rowCount; i++) {
                rows[i] = new Object[]{
                        (long) i + 1,
                        name,
                        i + 1,
                        price
                };
            }
            Connection connection = MockJdbc.connection(rows);
            statement = connection.prepareStatement("select ID, NAME, EDITION, PRICE from BOOK");
        }

        private void readRaw(Blackhole blackhole) throws SQLException {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    blackhole.consume(
                            new RawBook(
                                    resultSet.getLong(1),
                                    resultSet.getString(2),
                                    resultSet.getInt(3),
                                    resultSet.getBigDecimal(4)
                            )
                    );
                }
            }
        }

        private void readJimmer(Blackhole blackhole) throws SQLException {
            Reader.Context ctx = new Reader.Context(null, sqlClient);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    blackhole.consume(reader.read(resultSet, ctx));
                    ctx.resetCol();
                }
            }
        }
    }

    private static class RawBook {

        private final long id;

        private final String name;

        private final int edition;

        private final BigDecimal price;

        private RawBook(long id, String name, int edition, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.edition = edition;
            this.price = price;
        }
    }
}
