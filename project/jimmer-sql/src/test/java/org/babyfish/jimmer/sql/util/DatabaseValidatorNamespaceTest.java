package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
import org.babyfish.jimmer.sql.meta.DatabaseSchemaStrategy;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.model.validation.ValidationStore;
import org.babyfish.jimmer.sql.runtime.DatabaseValidators;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.h2.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DatabaseValidatorNamespaceTest {

    @Test
    public void testQualifiedDatabaseInCatalogMode() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", false);
            createTable(con, "TARGET_DATABASE", "VALIDATION_STORE", true);
            Connection catalogConnection = databaseConnection(con, true);

            Assertions.assertNull(validate(
                    catalogConnection,
                    new MySqlDialect(),
                    "TARGET_DATABASE.VALIDATION_STORE"
            ));
            // Finding the same table in the default database would miss CODE.
            assertMissingCode(validate(catalogConnection, new MySqlDialect(), "VALIDATION_STORE"));
        }
    }

    @Test
    public void testQualifiedDatabaseInSchemaMode() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", false);
            createTable(con, "TARGET_DATABASE", "VALIDATION_STORE", true);
            Connection schemaConnection = databaseConnection(con, false);

            Assertions.assertNull(validate(
                    schemaConnection,
                    new MySqlDialect(),
                    "TARGET_DATABASE.VALIDATION_STORE"
            ));
            assertMissingCode(validate(schemaConnection, new MySqlDialect(), "VALIDATION_STORE"));
        }
    }

    @Test
    public void testMissingQualifiedDatabaseDoesNotUseDefaultDatabase() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", true);
            for (boolean catalogMode : new boolean[]{true, false}) {
                DatabaseValidationException ex = validate(
                        databaseConnection(con, catalogMode), new MySqlDialect(), "MISSING.VALIDATION_STORE"
                );
                Assertions.assertNotNull(ex);
                Assertions.assertTrue(ex.getItems().get(0).getMessage().startsWith("There is no table"));
            }
        }
    }

    @Test
    public void testSchemaPatternDoesNotSelectAnotherDatabase() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "TARGETXDATABASE", "VALIDATION_STORE", true);
            DatabaseValidationException ex = validate(
                    databaseConnection(con, false), new MySqlDialect(), "TARGET_DATABASE.VALIDATION_STORE"
            );
            Assertions.assertNotNull(ex);
            Assertions.assertTrue(ex.getItems().get(0).getMessage().startsWith("There is no table"));
        }
    }

    @Test
    public void testUnclosedQuotedNameIsRejected() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", true);
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> validate(con, new H2Dialect(), "\"VALIDATION_STORE"));
        }
    }

    @Test
    public void testQuotedQualifiedNames() throws SQLException {
        String[][] names = {
                {"sample-db", "VALIDATION_STORE", "`sample-db`.`VALIDATION_STORE`"},
                {"TARGET_DATABASE", "VALIDATION_STORE", "\"TARGET_DATABASE\".\"VALIDATION_STORE\""},
                {"TARGET_DATABASE", "VALIDATION_STORE", "[TARGET_DATABASE].[VALIDATION_STORE]"},
                {"DB.NAME", "STORE.NAME", "\"DB.NAME\".\"STORE.NAME\""},
                {"DB\"NAME", "STORE\"NAME", "\"DB\"\"NAME\".\"STORE\"\"NAME\""},
                {"DB`NAME", "STORE`NAME", "`DB``NAME`.`STORE``NAME`"},
                {"DB]NAME", "STORE]NAME", "[DB]]NAME].[STORE]]NAME]"}
        };
        for (String[] name : names) {
            try (Connection con = openConnection()) {
                createTable(con, "PUBLIC", name[1], false);
                createTable(con, name[0], name[1], true);
                for (boolean catalogMode : new boolean[]{true, false}) {
                    Assertions.assertNull(
                            validate(databaseConnection(con, catalogMode), new MySqlDialect(), name[2]),
                            name[2] + ", catalogMode=" + catalogMode
                    );
                }
            }
        }
    }

    @Test
    public void testUnqualifiedNamesUseConnectionDatabase() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", true);
            createTable(con, "OTHER_DATABASE", "VALIDATION_STORE", false);
            for (boolean catalogMode : new boolean[]{true, false}) {
                Assertions.assertNull(validate(
                        databaseConnection(con, catalogMode),
                        new MySqlDialect(),
                        "VALIDATION_STORE"
                ));
            }
        }
    }

    @Test
    public void testH2SchemaAndThreePartNames() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "VALIDATION_STORE", false);
            createTable(con, "TARGET_SCHEMA", "VALIDATION_STORE", true);

            Assertions.assertNull(validate(con, new H2Dialect(), "TARGET_SCHEMA.VALIDATION_STORE"));
            Assertions.assertNull(validate(
                    con,
                    new H2Dialect(),
                    quote(con.getCatalog()) + ".\"TARGET_SCHEMA\".\"VALIDATION_STORE\""
            ));
            assertMissingCode(validate(con, new H2Dialect(), "VALIDATION_STORE"));

            con.setSchema("TARGET_SCHEMA");
            Assertions.assertNull(validate(con, new H2Dialect(), "VALIDATION_STORE"));
        }
    }

    @Test
    public void testQuotedDotsDoNotIntroduceNamespaces() throws SQLException {
        try (Connection con = openConnection()) {
            createTable(con, "PUBLIC", "STORE.NAME", true);
            createTable(con, "DB.NAME", "STORE.NAME", true);

            Assertions.assertNull(validate(con, new H2Dialect(), "\"STORE.NAME\""));
            Assertions.assertNull(validate(con, new H2Dialect(), "\"DB.NAME\".\"STORE.NAME\""));
            Assertions.assertNull(validate(
                    con,
                    new H2Dialect(),
                    quote(con.getCatalog()) + ".\"DB.NAME\".\"STORE.NAME\""
            ));
        }
    }

    private static DatabaseValidationException validate(Connection con, Dialect dialect, String tableName)
            throws SQLException {
        return DatabaseValidators.validate(
                new EntityManager(ValidationStore.class),
                "database-validation",
                true,
                new MetadataStrategy(
                        DatabaseSchemaStrategy.IMPLICIT,
                        new DefaultDatabaseNamingStrategy(false) {
                            @Override
                            public String tableName(ImmutableType type) {
                                return tableName;
                            }
                        },
                        ForeignKeyStrategy.REAL,
                        dialect,
                        prop -> null,
                        value -> value
                ),
                null,
                con
        );
    }

    private static void assertMissingCode(DatabaseValidationException ex) {
        Assertions.assertNotNull(ex);
        Assertions.assertTrue(ex.getItems().stream().anyMatch(item ->
                item.getProp() != null &&
                        item.getProp().getName().equals("code") &&
                        item.getMessage().startsWith("There is no column \"CODE\"")
        ), ex.getMessage());
    }

    private static Connection openConnection() throws SQLException {
        return new SimpleDriverDataSource(
                new Driver(),
                "jdbc:h2:mem:validation-namespace-" + UUID.randomUUID()
        ).getConnection();
    }

    private static void createTable(Connection con, String schema, String table, boolean withCode)
            throws SQLException {
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("create schema if not exists " + quote(schema));
            stmt.executeUpdate(
                    "create table " + quote(schema) + "." + quote(table) +
                            "(ID bigint not null primary key" +
                            (withCode ? ", CODE bigint not null" : "") + ")"
            );
        }
    }

    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    /**
     * Use H2 schemas as separate databases, exposing either catalog-based or
     * schema-based metadata. MariaDB reports the same capabilities in both modes.
     */
    private static Connection databaseConnection(Connection con, boolean catalogMode) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        DatabaseMetaData databaseMetadata = (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "supportsCatalogsInTableDefinitions":
                            return true;
                        case "supportsSchemasInTableDefinitions":
                            return false;
                        case "getTables":
                        case "getColumns":
                        case "getPrimaryKeys":
                            Object[] actualArgs = args.clone();
                            String database = (String) args[catalogMode ? 0 : 1];
                            if (database == null) {
                                database = "PUBLIC";
                            }
                            actualArgs[0] = con.getCatalog();
                            actualArgs[1] = database;
                            ResultSet resultSet = (ResultSet) invoke(metadata, method, actualArgs);
                            if (!method.getName().equals("getTables")) {
                                return resultSet;
                            }
                            return (ResultSet) Proxy.newProxyInstance(
                                    ResultSet.class.getClassLoader(),
                                    new Class<?>[]{ResultSet.class},
                                    (resultProxy, resultMethod, resultArgs) -> {
                                        if (resultMethod.getName().equals("getString")) {
                                            if ("TABLE_CAT".equals(resultArgs[0])) {
                                                return catalogMode ? resultSet.getString("TABLE_SCHEM") : "def";
                                            }
                                            if ("TABLE_SCHEM".equals(resultArgs[0])) {
                                                return catalogMode ? null : resultSet.getString("TABLE_SCHEM");
                                            }
                                        }
                                        return invoke(resultSet, resultMethod, resultArgs);
                                    }
                            );
                        default:
                            return invoke(metadata, method, args);
                    }
                }
        );
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMetaData":
                            return databaseMetadata;
                        case "getCatalog":
                            return catalogMode ? "PUBLIC" : "def";
                        case "getSchema":
                            return catalogMode ? null : "PUBLIC";
                        default:
                            return invoke(con, method, args);
                    }
                }
        );
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
