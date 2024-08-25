package org.babyfish.jimmer.sql.mutation;

import com.mysql.cj.MysqlConnection;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.TargetTransferMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * @see GetIdTest
 */
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
                       it.sql("insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?)");
                       it.batchVariables(0, "Develop", 0L);
                       it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(NAME, DELETED_MILLIS, DEPARTMENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, "Jacob", 0L, 100L);
                        it.batchVariables(1, "Tania", 0L, 100L);
                        it.batchVariables(2, "Oakes", 0L, 101L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Jacob\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," +
                                        "--->--->--->\"name\":\"Tania\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," +
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
                        it.sql(
                                "merge into DEPARTMENT(NAME, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) values(?, ?)"
                        );
                        it.batchVariables(0, "Market", 0L);
                        it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) values(?, ?, ?)"
                        );
                        it.batchVariables(0, "Jessica", 1L, 0L);
                        it.batchVariables(1, "Raines", 1L, 0L);
                        it.batchVariables(2, "Oakes", 100L, 0L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) " +
                                        "and DELETED_MILLIS = ?"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE,
                                1L,
                                new Object[]{ 2L, 100L},
                                0L
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE,
                                100L,
                                new Object[]{101L},
                                0L
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
    public void testInsertMySql() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.MYSQL_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new MySqlDialect()));
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
                NativeDatabases.MYSQL_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                                Arrays.asList(department1, department2)
                        ).setTargetTransferModeAll(TargetTransferMode.ALLOWED)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?)");
                        it.batchVariables(0, "Develop", 0L);
                        it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(NAME, DELETED_MILLIS, DEPARTMENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, "Jacob", 0L, 100L);
                        it.batchVariables(1, "Tania", 0L, 100L);
                        it.batchVariables(2, "Oakes", 0L, 101L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Jacob\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," +
                                        "--->--->--->\"name\":\"Tania\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," +
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
    public void testUpsertMySql() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.MYSQL_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.addScalarProvider(ScalarProvider.uuidByString());
        });
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
                NativeDatabases.MYSQL_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setTargetTransferModeAll(TargetTransferMode.ALLOWED),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?) " +
                                        "on duplicate key update " +
                                        "/* fake update to return all ids */ ID = last_insert_id(ID)"
                        );
                        it.batchVariables(0, "Market", 0L);
                        it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) values(?, ?, ?) " +
                                        "on duplicate key update " +
                                        "/* fake update to return all ids */ ID = last_insert_id(ID), " +
                                        "DEPARTMENT_ID = values(DEPARTMENT_ID)"
                        );
                        it.batchVariables(0, "Jessica", 1L, 0L);
                        it.batchVariables(1, "Raines", 1L, 0L);
                        it.batchVariables(2, "Oakes", 101L, 0L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE " +
                                        "set DELETED_MILLIS = ? " +
                                        "where " +
                                        "--->DEPARTMENT_ID in (?, ?) " +
                                        "and " +
                                        "--->(DEPARTMENT_ID, ID) not in ((?, ?), (?, ?), (?, ?)) " +
                                        "and " +
                                        "--->DELETED_MILLIS = ?"
                        );
                        it.variables(
                                UNKNOWN_VARIABLE,
                                1L,
                                101L,
                                1L,
                                2L,
                                1L,
                                101L,
                                101L,
                                102L,
                                0L
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

    @Test
    public void testInsertPostgres() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new PostgresDialect()));
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
                        it.sql("insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?)");
                        it.batchVariables(0, "Develop", 0L);
                        it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(NAME, DELETED_MILLIS, DEPARTMENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, "Jacob", 0L, 100L);
                        it.batchVariables(1, "Tania", 0L, 100L);
                        it.batchVariables(2, "Oakes", 0L, 101L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"100\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Jacob\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"100\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"101\"," +
                                        "--->--->--->\"name\":\"Tania\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"102\"," +
                                        "--->--->--->\"name\":\"Oakes\"," +
                                        "--->--->--->\"deletedMillis\":0," +
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
                                "insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?) " +
                                        "on conflict(NAME, DELETED_MILLIS) do update set " +
                                        "/* fake update to return all ids */ " +
                                        "DELETED_MILLIS = excluded.DELETED_MILLIS returning ID"
                        );
                        it.batchVariables(0, "Market", 0L);
                        it.batchVariables(1, "Sales", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) values(?, ?, ?) " +
                                        "on conflict(NAME, DELETED_MILLIS) " +
                                        "do update set DEPARTMENT_ID = excluded.DEPARTMENT_ID " +
                                        "returning ID"
                        );
                        it.batchVariables(0, "Jessica", 1L, 0L);
                        it.batchVariables(1, "Raines", 1L, 0L);
                        it.batchVariables(2, "Oakes", 101L, 0L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) " +
                                        "and DELETED_MILLIS = ?"
                        );
                        it.batchVariables(
                                0,
                                UNKNOWN_VARIABLE,
                                1L,
                                new Object[]{ 2L, 101L},
                                0L
                        );
                        it.batchVariables(
                                1,
                                UNKNOWN_VARIABLE,
                                101L,
                                new Object[]{102L},
                                0L
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
            String suffix = con instanceof MysqlConnection ?
                    "auto_increment = 100" :
                    "alter id restart with 100";
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table department " + suffix
                );
            }
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                        "alter table employee " + suffix
                );
            }
        });
    }
}
