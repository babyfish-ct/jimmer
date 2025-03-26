package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.DatabaseSchemaStrategy;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.ScalarTypeStrategy;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
import org.babyfish.jimmer.sql.model.issue918.Issue918Model;
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
}
