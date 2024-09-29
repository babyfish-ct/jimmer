package org.babyfish.jimmer.spring.dialect;

import org.babyfish.jimmer.sql.dialect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DialectDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialectDetector.class);

    @Nullable
    public static Dialect detectDialect(@NotNull Connection con) {
        try {
            String productName = JdbcUtils.commonDatabaseName(
                    extractDatabaseMetaData(con, DatabaseMetaData::getDatabaseProductName));
            DatabaseDriver driver = DatabaseDriver.fromProductName(productName);
            return getDialectForDriverOrNull(driver);
        } catch (MetaDataAccessException e) {
            LOGGER.warn("Failed to autodetect jimmer dialect", e);
            return null;
        }
    }

    private static <T> T extractDatabaseMetaData(@NotNull Connection con, @NotNull DatabaseMetaDataCallback<T> action)
            throws MetaDataAccessException {

        try {
            DatabaseMetaData metaData = con.getMetaData();
            if (metaData == null) {
                // should only happen in test environments
                throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
            }
            return action.processMetaData(metaData);
        } catch (CannotGetJdbcConnectionException ex) {
            throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
        } catch (SQLException ex) {
            throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
        } catch (AbstractMethodError err) {
            throw new MetaDataAccessException(
                    "JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
        }
    }

    @Nullable
    private static Dialect getDialectForDriverOrNull(@NotNull DatabaseDriver driver) {
        switch (driver) {
            case POSTGRESQL:
                return new PostgresDialect();
            case ORACLE:
                return new OracleDialect();
            case MYSQL:
                return new MySqlDialect();
            case SQLSERVER:
                return new SqlServerDialect();
            case H2:
                return new H2Dialect();
            default:
                return null;
        }
    }
}
