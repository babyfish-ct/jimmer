package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
import org.babyfish.jimmer.sql.meta.DatabaseSchemaStrategy;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.ScalarTypeStrategy;
import org.babyfish.jimmer.sql.model.issue918.Issue918Model;
import org.babyfish.jimmer.sql.model.validation.*;
import org.babyfish.jimmer.sql.runtime.DatabaseValidators;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.h2.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.function.Predicate;

public class DatabaseValidatorTest extends AbstractTest {

    @Test
    public void testH2() {
        jdbc(con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    EntityManager.fromResources(null, null),
                    "",
                    true,
                    new MetadataStrategy(
                            DatabaseSchemaStrategy.IMPLICIT,
                            DefaultDatabaseNamingStrategy.UPPER_CASE,
                            ForeignKeyStrategy.REAL,
                            new H2Dialect(),
                            new ScalarTypeStrategy() {
                                @Override
                                public Class<?> getOverriddenSqlType(ImmutableProp prop) {
                                    return null;
                                }
                            },
                            value -> {
                                switch (value) {
                                    case "${schema}":
                                        return "";
                                    case "${tables.player}":
                                        return "players";
                                    case "${columns.player.name}":
                                        return "player_name";
                                    case "${columns.player.teamId}":
                                        return "team_id";
                                }
                                return value;
                            }
                    ),
                    null,
                    con
            );
            Assertions.assertNull(ex);
        });
    }

    @Test
    public void testIssue918InH2() {
        jdbc(new SimpleDriverDataSource(
                new Driver(),
                "jdbc:h2:mem:issue_918;database_to_upper=true"
        ), false, con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    new EntityManager(Issue918Model.class),
                    "",
                    true,
                    new MetadataStrategy(
                            DatabaseSchemaStrategy.IMPLICIT,
                            DefaultDatabaseNamingStrategy.UPPER_CASE,
                            ForeignKeyStrategy.REAL,
                            new H2Dialect(),
                            prop -> null,
                            str -> str
                    ),
                    it -> true,
                    issue918InitH2(con) // create two schemas that both have the same table
            );
            Assertions.assertNull(ex);
        });
    }

    @Test
    public void testSingleTableInheritanceInH2() {
        jdbc(con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    new EntityManager(
                            org.babyfish.jimmer.sql.model.inheritance.singletable.Client.class,
                            org.babyfish.jimmer.sql.model.inheritance.singletable.Organization.class,
                            org.babyfish.jimmer.sql.model.inheritance.singletable.Person.class
                    ),
                    "",
                    true,
                    defaultH2Strategy(),
                    it -> true,
                    con
            );
            Assertions.assertNull(ex);
        });
    }

    @Test
    public void testJoinedInheritanceInH2() {
        jdbc(con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    new EntityManager(
                            org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client.class,
                            org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization.class,
                            org.babyfish.jimmer.sql.model.inheritance.joinedtable.Person.class,
                            org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientProject.class,
                            org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationProject.class
                    ),
                    "",
                    true,
                    defaultH2Strategy(),
                    it -> true,
                    con
            );
            Assertions.assertNull(ex);
        });
    }

    @Test
    public void testPrimaryKeyColumns() throws SQLException {
        for (String primaryKey : new String[] {"", ", primary key(code)", ", primary key(id)"}) {
            DatabaseValidationException ex = validateH2(
                    "create table VALIDATION_STORE(id bigint not null, code bigint not null" + primaryKey + ")",
                    null,
                    ValidationStore.class
            );
            if (primaryKey.equals(", primary key(id)")) {
                Assertions.assertNull(ex);
            } else {
                Assertions.assertNotNull(ex);
                Assertions.assertEquals(1, ex.getItems().size());
                assertProblem(ex, ValidationStore.class, null,
                        "Expected primary key columns are [ID], but actual primary key columns are " +
                                (primaryKey.isEmpty() ? "[]" : "[CODE]"));
            }
        }
    }

    @Test
    public void testCompositePrimaryKeyColumns() throws SQLException {
        for (String primaryKey : new String[] {"", ", primary key(ID_X)", ", primary key(ID_Y, ID_X)"}) {
            DatabaseValidationException ex = validateH2(
                    "create table VALIDATION_COMPOSITE(ID_X varchar(50) not null, ID_Y varchar(50) not null" + primaryKey + ")",
                    null,
                    ValidationComposite.class
            );
            if (primaryKey.equals(", primary key(ID_Y, ID_X)")) {
                Assertions.assertNull(ex);
            } else {
                Assertions.assertNotNull(ex);
                Assertions.assertEquals(1, ex.getItems().size());
                assertProblem(ex, ValidationComposite.class, null,
                        "Expected primary key columns are [ID_X, ID_Y], but actual primary key columns are " +
                                (primaryKey.isEmpty() ? "[]" : "[ID_X]"));
            }
        }
    }

    @Test
    public void testIgnoredId() throws SQLException {
        for (String primaryKey : new String[] {"", ", primary key(OTHER_ID)"}) {
            Assertions.assertNull(validateH2(
                    "create table VALIDATION_IGNORED_ID(ID bigint, OTHER_ID bigint not null" + primaryKey + ")",
                    null,
                    ValidationIgnoredId.class
            ));
        }
    }

    @Test
    public void testMissingPrimaryAndForeignKeys() throws SQLException {
        assertMissingConstraints(validateH2(validationSchema(false), null, validationTypes()));
    }

    @Test
    public void testForeignKeyWithDefaultAndExplicitPredicate() throws SQLException {
        for (Predicate<ImmutableType> predicate : Arrays.<Predicate<ImmutableType>>asList(null, it -> true)) {
            Assertions.assertNull(validateH2(validationSchema(true), predicate, validationTypes()));
            DatabaseValidationException ex = validateH2(
                    validationSchema(true) + ";alter table VALIDATION_BOOK drop constraint FK_BOOK_STORE",
                    predicate,
                    validationTypes()
            );
            Assertions.assertNotNull(ex);
            Assertions.assertEquals(1, ex.getItems().size());
            assertProblem(ex, ValidationBook.class, "store", "No foreign key constraint for columns: [STORE_ID]");
        }
    }

    @Test
    public void testForeignKeyReferencesWrongColumn() throws SQLException {
        DatabaseValidationException ex = validateH2(
                validationSchema(true) +
                        ";alter table VALIDATION_BOOK drop constraint FK_BOOK_STORE" +
                        ";alter table VALIDATION_BOOK add constraint FK_BOOK_STORE foreign key(STORE_ID) references VALIDATION_STORE(CODE)",
                null,
                validationTypes()
        );
        Assertions.assertNotNull(ex);
        Assertions.assertEquals(1, ex.getItems().size());
        assertProblem(ex, ValidationBook.class, "store", "Illegal foreign key \"FK_BOOK_STORE\", referenced column(s) is [CODE]");
    }

    @Test
    public void testExcludedTarget() throws SQLException {
        Assertions.assertNull(validateH2(
                validationSchema(true) +
                        ";alter table VALIDATION_BOOK drop constraint FK_BOOK_STORE" +
                        ";alter table VALIDATION_BOOK_STORE_MAPPING drop constraint FK_MAPPING_STORE" +
                        ";drop table VALIDATION_STORE",
                type -> type.getJavaClass() != ValidationStore.class,
                validationTypes()
        ));
        Assertions.assertNull(validateH2(validationSchema(false), it -> false, validationTypes()));
    }

    @Test
    public void testIgnoredTarget() throws SQLException {
        Assertions.assertNull(validateH2(
                validationSchema(true) +
                        ";alter table VALIDATION_BOOK drop constraint FK_IGNORED_TARGET" +
                        ";drop table VALIDATION_IGNORED_STORE",
                null,
                validationTypes()
        ));
    }

    @Test
    public void testMiddleTableConstraints() throws SQLException {
        DatabaseValidationException ex = validateH2(
                validationSchema(true) +
                        ";alter table VALIDATION_BOOK_STORE_MAPPING drop constraint FK_MAPPING_BOOK" +
                        ";alter table VALIDATION_BOOK_STORE_MAPPING drop constraint FK_MAPPING_STORE" +
                        ";alter table VALIDATION_BOOK_STORE_MAPPING drop constraint PK_MAPPING" +
                        ";alter table VALIDATION_BOOK_STORE_MAPPING add primary key(BOOK_ID)",
                null,
                validationTypes()
        );
        Assertions.assertNotNull(ex);
        Assertions.assertEquals(3, ex.getItems().size());
        assertProblem(ex, ValidationBook.class, "stores", "No foreign key constraint for columns: [BOOK_ID]");
        assertProblem(ex, ValidationBook.class, "stores", "No foreign key constraint for columns: [STORE_ID]");
        assertProblem(ex, ValidationBook.class, "stores", "The primary key of middle table must contain all columns, " +
                "but column \"STORE_ID\" of table \"VALIDATION_BOOK_STORE_MAPPING\" is not part of the primary key");
    }

    @Test
    public void testMissingConstraintsInPostgres() {
        NativeDatabases.assumeNativeDatabase();
        jdbc(NativeDatabases.POSTGRES_DATA_SOURCE, true, con -> {
            executeDdl(con, "create schema issue_1319_validation");
            con.setSchema("issue_1319_validation");
            executeDdl(con, validationSchema(false));
            assertMissingConstraints(validate(con, new PostgresDialect(), null, validationTypes()));
            executeDdl(con,
                    "alter table VALIDATION_STORE add primary key(ID);" +
                            "alter table VALIDATION_BOOK add primary key(ID);" +
                            "alter table VALIDATION_BOOK add foreign key(STORE_ID) references VALIDATION_STORE(ID);" +
                            "alter table VALIDATION_BOOK_STORE_MAPPING add primary key(BOOK_ID, STORE_ID);" +
                            "alter table VALIDATION_BOOK_STORE_MAPPING add foreign key(BOOK_ID) references VALIDATION_BOOK(ID);" +
                            "alter table VALIDATION_BOOK_STORE_MAPPING add foreign key(STORE_ID) references VALIDATION_STORE(ID)"
            );
            Assertions.assertNull(validate(con, new PostgresDialect(), null, validationTypes()));
        });
    }

    private static DatabaseValidationException validateH2(
            String ddl,
            Predicate<ImmutableType> predicate,
            Class<?>... types
    ) throws SQLException {
        try (Connection con = new SimpleDriverDataSource(new Driver(), "jdbc:h2:mem:validation").getConnection()) {
            executeDdl(con, ddl);
            return validate(con, new H2Dialect(), predicate, types);
        }
    }

    private static DatabaseValidationException validate(
            Connection con,
            Dialect dialect,
            Predicate<ImmutableType> predicate,
            Class<?>... types
    ) throws SQLException {
        return DatabaseValidators.validate(
                new EntityManager(types),
                "database-validation",
                true,
                new MetadataStrategy(DatabaseSchemaStrategy.IMPLICIT, DefaultDatabaseNamingStrategy.UPPER_CASE,
                        ForeignKeyStrategy.REAL, dialect, prop -> null, str -> str),
                predicate,
                con
        );
    }

    private static void executeDdl(Connection con, String ddl) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            for (String sql : ddl.split(";")) {
                stmt.executeUpdate(sql);
            }
        }
    }

    private static Class<?>[] validationTypes() {
        return new Class<?>[] {ValidationStore.class, ValidationBook.class, ValidationIgnoredStore.class};
    }

    private static String validationSchema(boolean constraints) {
        return "create table VALIDATION_STORE(ID bigint not null, CODE bigint not null unique" +
                (constraints ? ", constraint PK_STORE primary key(ID)" : "") + ");" +
                "create table VALIDATION_IGNORED_STORE(ID bigint primary key);" +
                "create table VALIDATION_BOOK(ID bigint not null, STORE_ID bigint not null, FAKE_STORE_ID bigint not null, " +
                "IGNORED_STORE_ID bigint not null, IGNORED_TARGET_ID bigint not null, " +
                "constraint FK_IGNORED_TARGET foreign key(IGNORED_TARGET_ID) references VALIDATION_IGNORED_STORE(ID)" +
                (constraints ? ", constraint PK_BOOK primary key(ID), " +
                        "constraint FK_BOOK_STORE foreign key(STORE_ID) references VALIDATION_STORE(ID)" : "") + ");" +
                "create table VALIDATION_BOOK_STORE_MAPPING(BOOK_ID bigint not null, STORE_ID bigint not null" +
                (constraints ? ", constraint PK_MAPPING primary key(BOOK_ID, STORE_ID), " +
                        "constraint FK_MAPPING_BOOK foreign key(BOOK_ID) references VALIDATION_BOOK(ID), " +
                        "constraint FK_MAPPING_STORE foreign key(STORE_ID) references VALIDATION_STORE(ID)" : "") + ")";
    }

    private static void assertMissingConstraints(DatabaseValidationException ex) {
        Assertions.assertNotNull(ex);
        Assertions.assertEquals(7, ex.getItems().size());
        assertProblem(ex, ValidationStore.class, null, "Expected primary key columns are [ID], but actual primary key columns are []");
        assertProblem(ex, ValidationBook.class, null, "Expected primary key columns are [ID], but actual primary key columns are []");
        assertProblem(ex, ValidationBook.class, "store", "No foreign key constraint for columns: [STORE_ID]");
        assertProblem(ex, ValidationBook.class, "stores", "No foreign key constraint for columns: [BOOK_ID]");
        assertProblem(ex, ValidationBook.class, "stores", "No foreign key constraint for columns: [STORE_ID]");
        for (String column : new String[] {"BOOK_ID", "STORE_ID"}) {
            assertProblem(ex, ValidationBook.class, "stores", "The primary key of middle table must contain all columns, " +
                    "but column \"" + column + "\" of table \"VALIDATION_BOOK_STORE_MAPPING\" is not part of the primary key");
        }
    }

    private static void assertProblem(DatabaseValidationException ex, Class<?> type, String prop, String message) {
        Assertions.assertTrue(ex.getItems().stream().anyMatch(item ->
                item.getType().getJavaClass() == type &&
                        (prop == null ? item.getProp() == null : item.getProp() != null && prop.equals(item.getProp().getName())) &&
                        item.getMessage().startsWith(message)), ex.getMessage());
    }

    private static Connection issue918InitH2(Connection con) throws SQLException {
        String DDL = "create table ${schema}.issue918_model(\n" +
                "    id bigint auto_increment not null,\n" +
                "    name varchar(50) not null\n" +
                ");\n" +
                "alter table ${schema}.issue918_model\n" +
                "    add constraint pk_issue918_model\n" +
                "        primary key(id);\n";
        DDL = "create schema A;\n" +
                "create schema B;\n" +
                DDL.replace("${schema}", "A") +
                DDL.replace("${schema}", "B");
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(DDL);
        }
        con.setSchema("A");
        return con;
    }

    private static MetadataStrategy defaultH2Strategy() {
        return new MetadataStrategy(
                DatabaseSchemaStrategy.IMPLICIT,
                DefaultDatabaseNamingStrategy.UPPER_CASE,
                ForeignKeyStrategy.REAL,
                new H2Dialect(),
                prop -> null,
                str -> str
        );
    }
}
