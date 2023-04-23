package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assumptions;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class NativeDatabases {

    private NativeDatabases() {}

    public static void assumeNativeDatabase() {

        String nativeDb = System.getenv("jimmer-sql-test-native-database");
        Assumptions.assumeTrue(
                nativeDb != null && !nativeDb.isEmpty() && !"false".equals(nativeDb)
        );
    }

    public static void assumeOracleDatabase() {

        Assumptions.assumeTrue(
                "oracle".equals(System.getenv("jimmer-sql-test-native-database"))
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

    public static final DataSource ORACLE_DATA_SOURCE =
            new SimpleDriverDataSource(
                    new oracle.jdbc.driver.OracleDriver(),
                    "jdbc:oracle:thin:@//DESKTOP-O2AON5I:1521/orcl",
                    "jimmer",
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
