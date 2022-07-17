package org.babyfish.jimmer.benchmark;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize(int dataCount) throws SQLException, IOException {
        if (dataCount < 1) {
            throw new IllegalArgumentException("dataCount is less than 1");
        }
        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            create(con);
            insert(con, dataCount);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    private void create(Connection con) throws IOException, SQLException {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(
                        DatabaseInitializer.class.getClassLoader().getResourceAsStream("h2-database.sql")
                )
            )
        ) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
        }
        try (Statement stmt = con.createStatement()) {
            stmt.execute(builder.toString());
        }
    }

    private void insert(Connection con, int dataCount) throws SQLException {
        String sql = "insert into data(id, " +
                IntStream.range(1, 10).mapToObj(i -> "value_" + i).collect(Collectors.joining(", ")) +
                ") values(" +
                IntStream.range(0, 10).mapToObj(i -> "?").collect(Collectors.joining(", ")) +
                ")";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int row = 1; row <= dataCount; row++) {
                stmt.setLong(1, row);
                for (int col = 2; col <= 10; col++) {
                    stmt.setInt(col, ThreadLocalRandom.current().nextInt(100));
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
