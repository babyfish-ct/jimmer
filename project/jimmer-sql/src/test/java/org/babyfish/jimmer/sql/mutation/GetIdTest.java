package org.babyfish.jimmer.sql.mutation;

import com.mysql.cj.MysqlConnection;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeDraft;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.Arrays;

/**
 * @see IdentityTest
 */
public class GetIdTest extends AbstractMutationTest {

    @Test
    public void testUpdateNothingAndGetIdFromH2() {

        resetIdentity(null);

        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new H2Dialect());
            it.setTargetTransferable(true);
        });
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica");
            });
        });

        executeAndExpectResult(
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = any(?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables((Object) new Object[] { "Sales", "Market" }, 0L);
                        it.queryReason(QueryReason.GET_ID_WHEN_UPDATE_NOTHING);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) values(?, ?, ?)"
                        );
                        it.batchVariables(0, "Jacob", 1L, 0L);
                        it.batchVariables(1, "Jessica", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where " +
                                        "--->DEPARTMENT_ID = ? " +
                                        "and " +
                                        "--->not (ID = any(?)) " +
                                        "and " +
                                        "--->DELETED_MILLIS = ?"
                        );
                        it.variables(UNKNOWN_VARIABLE, 1L, new Object[] {100L, 2L}, 0L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Sales\",\"employees\":[{\"name\":\"Oakes\"}]}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{\"id\":\"100\",\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}," +
                                        "--->--->{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateAndGetIdFromH2() {
        resetIdentity(null);
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Employee employee1 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jacob");
            draft.setDepartmentId(1L);
        });
        Employee employee2 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jessica");
            draft.setDepartmentId(1L);
        });
        Employee employee3 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Raines");
            draft.setDepartmentId(1L);
        });
        Employee employee4 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Sam");
            draft.setDepartmentId(1L);
        });
        executeAndExpectResult(
                sqlClient
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(employee1, employee2, employee3, employee4)
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update EMPLOYEE set DEPARTMENT_ID = ? where NAME = ?");
                        it.batchVariables(0, 1L, "Jacob");
                        it.batchVariables(1, 1L, "Jessica");
                        it.batchVariables(2, 1L, "Raines");
                        it.batchVariables(3, 1L, "Sam");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Raines\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"1\",\"name\":\"Sam\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateNothingAndGetIdFromMySql() {

        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.MYSQL_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setTargetTransferable(true);
            it.addScalarProvider(ScalarProvider.uuidByString());
        });
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica");
            });
        });

        executeAndExpectResult(
                NativeDatabases.MYSQL_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME in (?, ?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Sales", "Market", 0L);
                        it.queryReason(QueryReason.GET_ID_WHEN_UPDATE_NOTHING);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) values(?, ?, ?) " +
                                        "on duplicate key update " +
                                        "/* fake update to return all ids */ ID = last_insert_id(ID), " +
                                        "DEPARTMENT_ID = values(DEPARTMENT_ID)"
                        );
                        it.batchVariables(0, "Jacob", 1L, 0L);
                        it.batchVariables(1, "Jessica", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where " +
                                        "--->DEPARTMENT_ID = ? " +
                                        "and " +
                                        "--->ID not in (?, ?) " +
                                        "and " +
                                        "--->DELETED_MILLIS = ?"
                        );
                        it.variables(UNKNOWN_VARIABLE, 1L, 100L, 2L, 0L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Sales\",\"employees\":[{\"name\":\"Oakes\"}]}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{\"id\":\"100\",\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}," +
                                        "--->--->{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateAndGetIdFromMySql() {
        resetIdentity(NativeDatabases.MYSQL_DATA_SOURCE);
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new MySqlDialect()));
        Employee employee1 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jacob");
            draft.setDepartmentId(1L);
        });
        Employee employee2 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jessica");
            draft.setDepartmentId(1L);
        });
        Employee employee3 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Raines");
            draft.setDepartmentId(1L);
        });
        Employee employee4 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Sam");
            draft.setDepartmentId(1L);
        });
        executeAndExpectResult(
                NativeDatabases.MYSQL_DATA_SOURCE,
                sqlClient
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(employee1, employee2, employee3, employee4)
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set ID = last_insert_id(ID), DEPARTMENT_ID = ? " +
                                        "where NAME = ?"
                        );
                        it.batchVariables(0, 1L, "Jacob");
                        it.batchVariables(1, 1L, "Jessica");
                        it.batchVariables(2, 1L, "Raines");
                        it.batchVariables(3, 1L, "Sam");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Raines\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"1\",\"name\":\"Sam\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateNothingAndGetIdFromPostgres() {

        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE);

        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new PostgresDialect());
            it.setTargetTransferable(true);
        });
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setName("Oakes");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setName("Market");
            draft.addIntoEmployees(emp -> {
                emp.setName("Jacob");
            });
            draft.addIntoEmployees(emp -> {
                emp.setName("Jessica");
            });
        });

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = any(?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables(new Object[] { "Sales", "Market" }, 0L);
                        it.queryReason(QueryReason.GET_ID_WHEN_UPDATE_NOTHING);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, DEPARTMENT_ID, DELETED_MILLIS) values(?, ?, ?) " +
                                        "on conflict(NAME, DELETED_MILLIS) do update " +
                                        "set DEPARTMENT_ID = excluded.DEPARTMENT_ID " +
                                        "returning ID"
                        );
                        it.batchVariables(0, "Jacob", 1L, 0L);
                        it.batchVariables(1, "Jessica", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE set DELETED_MILLIS = ? " +
                                        "where " +
                                        "--->DEPARTMENT_ID = ? " +
                                        "and " +
                                        "--->not (ID = any(?)) " +
                                        "and " +
                                        "--->DELETED_MILLIS = ?"
                        );
                        it.variables(UNKNOWN_VARIABLE, 1L, new Object[] {100L, 2L}, 0L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Sales\",\"employees\":[{\"name\":\"Oakes\"}]}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{\"id\":\"100\",\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}," +
                                        "--->--->{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateAndGetIdFromMyPostgres() {
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE);
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new PostgresDialect()));
        Employee employee1 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jacob");
            draft.setDepartmentId(1L);
        });
        Employee employee2 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jessica");
            draft.setDepartmentId(1L);
        });
        Employee employee3 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Raines");
            draft.setDepartmentId(1L);
        });
        Employee employee4 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Sam");
            draft.setDepartmentId(1L);
        });
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                sqlClient
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(employee1, employee2, employee3, employee4)
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set ID = ID, DEPARTMENT_ID = ? " +
                                        "where NAME = ? " +
                                        "returning ID"
                        );
                        it.batchVariables(0, 1L, "Jacob");
                        it.batchVariables(1, 1L, "Jessica");
                        it.batchVariables(2, 1L, "Raines");
                        it.batchVariables(3, 1L, "Sam");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Raines\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"1\",\"name\":\"Sam\",\"department\":{\"id\":\"1\"}}"
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

    @Test
    public void testUpdateAndGetIdFromWeakDatabase() {
        resetIdentity(null);
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new H2Dialect() {
                @Override
                public boolean isIdFetchableByKeyUpdate() {
                    return false;
                }
            });
        });
        Employee employee1 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jacob");
            draft.setDepartmentId(1L);
        });
        Employee employee2 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Jessica");
            draft.setDepartmentId(1L);
        });
        Employee employee3 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Raines");
            draft.setDepartmentId(1L);
        });
        Employee employee4 = EmployeeDraft.$.produce(draft -> {
            draft.setName("Sam");
            draft.setDepartmentId(1L);
        });
        executeAndExpectResult(
                sqlClient
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(employee1, employee2, employee3, employee4)
                        )
                        .setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.NAME = any(?) and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables(new Object[]{"Jacob", "Jessica", "Raines", "Sam"}, 0L);
                        it.queryReason(QueryReason.GET_ID_FOR_KEY_BASE_UPDATE);
                    });
                    ctx.statement(it -> {
                        it.sql("update EMPLOYEE set DEPARTMENT_ID = ? where NAME = ?");
                        it.batchVariables(0, 1L, "Jessica");
                        it.batchVariables(1, 1L, "Sam");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Jacob\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"2\",\"name\":\"Jessica\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"name\":\"Raines\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":\"1\",\"name\":\"Sam\",\"department\":{\"id\":\"1\"}}"
                        );
                    });
                }
        );
    }
}
