package org.babyfish.jimmer.sql.common;

import org.junit.jupiter.api.Assumptions;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
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
                "oracle".equals(System.getenv("jimmer-sql-test-native-database")) &&
                        ORACLE_DATA_SOURCE != null
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

    public static final DataSource ORACLE_DATA_SOURCE;

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
        Class<?> oracleDriverClass;
        try {
            oracleDriverClass = Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException ex) {
            oracleDriverClass = null;
        }
        if (oracleDriverClass != null) {
            Driver driver;
            try {
                driver = (Driver) oracleDriverClass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                throw new RuntimeException("Cannot create oracle driver", ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("Cannot create oracle driver", ex.getTargetException());
            }
            ORACLE_DATA_SOURCE = new SimpleDriverDataSource(
                    driver,
                    "jdbc:oracle:thin:@//DESKTOP-O2AON5I:1521/orcl",
                    "jimmer",
                    "123456"
            );
        } else {
            ORACLE_DATA_SOURCE = null;
        }
    }
}
