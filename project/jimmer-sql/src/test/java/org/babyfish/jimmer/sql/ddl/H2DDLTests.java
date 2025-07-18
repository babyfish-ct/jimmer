package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.dialect.*;
import org.babyfish.jimmer.sql.model.Tables;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author honhimW
 */

public class H2DDLTests extends AbstractDDLTest {

    enum Mode {
        H2, SQLITE,
        POSTGRESQL("PostgreSQL"),
        ORACLE("Oracle"),
        SQL_SERVER("MSSQLServer"),
        MYSQL("MySQL"),
        TI_DB("MySQL"),
        MARIADB("MySQL"),
        ;

        private final String mode;

        Mode() {
            this.mode = "Regular";
        }
        Mode(String mode) {
            this.mode = mode;
        }
    }

    DataSource newDataSource(Mode mode) {
        JdbcDataSource dataSource = new JdbcDataSource();

        dataSource.setURL(String.format("jdbc:h2:mem:test;MODE\\=%s", mode.mode));
        return dataSource;
    }

    Dialect dialect(Mode mode) {
        Dialect dialect;
        switch (mode) {
            case SQLITE:
                dialect = new SQLiteDialect();
                break;
            case POSTGRESQL:
                dialect = new PostgresDialect();
                break;
            case ORACLE:
                dialect = new OracleDialect();
                break;
            case SQL_SERVER:
                dialect = new SqlServerDialect();
                break;
            case MYSQL:
                dialect = new MySqlDialect();
                break;
            case TI_DB:
                dialect = new TiDBDialect();
                break;
            case MARIADB:
                dialect = new MySqlDialect();
                break;
            default:
                dialect = new H2Dialect();
                break;
        }
        return  dialect;
    }

    @Test
    void allInOne() {
        Mode[] modes = Mode.values();
        for (Mode mode : modes) {
            DataSource inMemoryDataSource = newDataSource(mode);
            Dialect dialect = dialect(mode);
            if (!dialect.isForeignKeySupported()) {
                // test tables contains foreign-key
                continue;
            }
            System.out.println("############" + mode.name() + "############");
            JSqlClientImplementor sqlClient = getSqlClient(builder -> builder
                .setDialect(dialect)
                .setConnectionManager(testConnectionManager())
                .setSqlFormatter(SqlFormatter.PRETTY)
            );
            SchemaCreator schemaCreator = new SchemaCreator(sqlClient, DatabaseVersion.LATEST);
            schemaCreator.init();
            List<Table<?>> tables = new ArrayList<>();
            tables.add(Tables.AUTHOR_TABLE);
            tables.add(Tables.BOOK_STORE_TABLE);
            tables.add(Tables.BOOK_TABLE);
            tables.add(Tables.COUNTRY_TABLE);
            tables.add(Tables.ORGANIZATION_TABLE);
            List<ImmutableType> types = tables.stream().map(TableTypeProvider::getImmutableType).collect(Collectors.toList());

            // create sql statements
            List<String> sqlCreateStrings = schemaCreator.getSqlCreateStrings(types);
            Collections.reverse(types);
            // drop sql statements
            List<String> sqlDropStrings = schemaCreator.getSqlDropStrings(types);

            Assertions.assertDoesNotThrow(() -> {
                testConnectionManager().execute(inMemoryDataSource.getConnection(), connection -> {
                    for (String sqlCreateString : sqlCreateStrings) {
                        System.out.println(sqlCreateString);
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlCreateString)) {
                            preparedStatement.execute();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                });
            });

            for (ImmutableType type : types) {
                Assertions.assertDoesNotThrow(() -> {
                    testConnectionManager().execute(inMemoryDataSource.getConnection(), connection -> {
                        try {
                            ResultSet resultSet = connection.getMetaData().getColumns(null, null, type.getTableName(sqlClient.getMetadataStrategy()), null);
                            List<Map<String, Object>> resultMap = toMap(resultSet);
                            Map<String, Map<String, Object>> collect = resultMap.stream().collect(Collectors.toMap(map -> (String) map.get("COLUMN_NAME"), map -> map));
                            assertColumnTypes(type, collect);
                            return null;
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
            }
            System.out.println("============== drop ================");
            Assertions.assertDoesNotThrow(() -> {
                testConnectionManager().execute(inMemoryDataSource.getConnection(), connection -> {
                    for (String sqlDropString : sqlDropStrings) {
                        System.out.println(sqlDropString);
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlDropString)) {
                            preparedStatement.execute();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                });
            });
        }
    }

}
