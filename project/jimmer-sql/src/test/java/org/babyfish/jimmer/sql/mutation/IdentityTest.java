package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.TargetTransferMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.PgConnection;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

public class IdentityTest extends AbstractMutationTest {

    @Test
    public void testInsertH2() {

        resetIdentity(null);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Develop");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Tania");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });
        executeAndExpectResult(
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                       it.sql("insert into DEPARTMENT(NAME, DELETED_TIME) values(?, ?)");
                       it.batchVariables(0, "Develop", new DbLiteral.DbNull(LocalDateTime.class));
                       it.batchVariables(1, "Sales", new DbLiteral.DbNull(LocalDateTime.class));
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(NAME, DELETED_UUID, DEPARTMENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, "Jacob", new DbLiteral.DbNull(UUID.class), 100L);
                        it.batchVariables(1, "Tania", new DbLiteral.DbNull(UUID.class), 100L);
                        it.batchVariables(2, "Oakes", new DbLiteral.DbNull(UUID.class), 101L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedTime\":null," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Jacob\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," +
                                        "--->--->--->\"name\":\"Tania\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"101\"," +
                                        "--->\"name\":\"Sales\"," +
                                        "--->\"deletedTime\":null," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," +
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"101\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpsertH2() {

        resetIdentity(null);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market"); // Exists(id = 1)
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica"); // Exists(id = 2)
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Raines"); // Not Exists
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales"); // Not Exists
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes"); // Not Exists
            });
        });
        executeAndExpectResult(
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setTargetTransferModeAll(TargetTransferMode.ALLOWED),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into DEPARTMENT(NAME) key(NAME) values(?)");
                        it.batchVariables(0, "Market");
                        it.batchVariables(1, "Sales");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into EMPLOYEE(NAME, DEPARTMENT_ID) key(NAME) values(?, ?)");
                        it.batchVariables(0, "Jessica", 1L);
                        it.batchVariables(1, "Raines", 1L);
                        it.batchVariables(2, "Oakes", 100L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE set DELETED_UUID = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) " +
                                        "and DELETED_UUID is null"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE,
                                1L,
                                new Object[]{ 2L, 100L}
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE,
                                100L,
                                new Object[]{101L}
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," + // Old id
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"2\"," + // Old id
                                        "--->--->--->\"name\":\"Jessica\"," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"100\"," + // Allocated Id
                                        "--->--->--->\"name\":\"Raines\"," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," + // Allocated Id
                                        "--->\"name\":\"Sales\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"101\"," + // Allocated Id
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testInsertPostgres() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Develop");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Tania");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                                Arrays.asList(department1, department2)
                        ).setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into DEPARTMENT(NAME, DELETED_TIME) values(?, ?)");
                        it.batchVariables(0, "Develop", new DbLiteral.DbNull(LocalDateTime.class));
                        it.batchVariables(1, "Sales", new DbLiteral.DbNull(LocalDateTime.class));
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(NAME, DELETED_UUID, DEPARTMENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, "Jacob", new DbLiteral.DbNull(UUID.class), 100L);
                        it.batchVariables(1, "Tania", new DbLiteral.DbNull(UUID.class), 100L);
                        it.batchVariables(2, "Oakes", new DbLiteral.DbNull(UUID.class), 101L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedTime\":null," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Jacob\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," +
                                        "--->--->--->\"name\":\"Tania\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"101\"," +
                                        "--->\"name\":\"Sales\"," +
                                        "--->\"deletedTime\":null," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," +
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"deletedUUID\":null," +
                                        "--->--->--->\"department\":{\"id\":\"101\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpsertPostgres() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new PostgresDialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market"); // Exists(id = 1)
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica"); // Exists(id = 2)
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Raines"); // Not Exists
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales"); // Not Exists
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes"); // Not Exists
            });
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setTargetTransferModeAll(TargetTransferMode.ALLOWED),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into DEPARTMENT(NAME) values(?) " +
                                        "on conflict(NAME) do update set " +
                                        "/* fake update to return all ids */ NAME = excluded.NAME returning id"
                        );
                        it.batchVariables(0, "Market");
                        it.batchVariables(1, "Sales");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, DEPARTMENT_ID) values(?, ?) " +
                                        "on conflict(NAME) do update set DEPARTMENT_ID = excluded.DEPARTMENT_ID " +
                                        "returning id"
                        );
                        it.batchVariables(0, "Jessica", 1L);
                        it.batchVariables(1, "Raines", 1L);
                        it.batchVariables(2, "Oakes", 101L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE set DELETED_UUID = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) " +
                                        "and DELETED_UUID is null"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE,
                                1L,
                                new Object[]{ 2L, 101L}
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE,
                                101L,
                                new Object[]{102L}
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," + // Old id
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"2\"," + // Old id
                                        "--->--->--->\"name\":\"Jessica\"," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," + // Allocated Id
                                        "--->--->--->\"name\":\"Raines\"," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"101\"," + // Allocated Id
                                        "--->\"name\":\"Sales\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," + // Allocated Id
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"department\":{\"id\":\"101\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    private void resetIdentity(DataSource dataSource) {
        jdbc(dataSource, false, con -> {
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table department alter id restart with 100"
                );
            }
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table employee alter id restart with 100"
                );
            }
        });
    }
}
