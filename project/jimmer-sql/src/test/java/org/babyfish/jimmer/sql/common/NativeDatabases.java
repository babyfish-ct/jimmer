package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assumptions;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class NativeDatabases {

    private NativeDatabases() {}

    public static void assumeNativeDatabase() {

        Assumptions.assumeTrue(
                "true".equals(System.getenv("jimmer-sql-test-native-database"))
        );
    }

    public static final DataSource MYSQL_DATA_SOURCE;

    public static final DataSource POSTGRES_DATA_SOURCE =
            new SimpleDriverDataSource(
                    new org.postgresql.Driver(),
                    "jdbc:postgresql://localhost:5432/db",
                    "sa",
                    "123456"
            );

    static {
        try {
            MYSQL_DATA_SOURCE = new SimpleDriverDataSource(
                    new com.mysql.cj.jdbc.Driver(),
                    "jdbc:mysql://localhost:3306/kimmer",
                    "root",
                    "123456"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Cannot init mysql data source");
        }
    }
}
