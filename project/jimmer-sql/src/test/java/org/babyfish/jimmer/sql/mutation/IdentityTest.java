package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.TargetTransferMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class IdentityTest extends AbstractMutationTest {

    @Test
    public void upsertH2WithId() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect()));
        Department department1 = DepartmentDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setName("NewMarket");
            draft.addIntoEmployees(emp -> {
                emp.setId(2L);
                emp.setName("Linda");
            });
            draft.addIntoEmployees(emp -> {
                emp.setId(20L);
                emp.setName("Raines");
            });
        });
        Department department2 = DepartmentDraft.$.produce(draft -> {
            draft.setId(10L);
            draft.setName("Sales");
            draft.addIntoEmployees(emp -> {
                emp.setId(40L);
                emp.setName("Oakes");
            });
        });
        executeAndExpectResult(
                sqlClient.getEntities().saveEntitiesCommand(
                        Arrays.asList(department1, department2)
                ).setTargetTransferModeAll(TargetTransferMode.ALLOWED),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into DEPARTMENT(ID, NAME) key(ID) values(?, ?)");
                        it.batchVariables(0, 1L, "NewMarket");
                        it.batchVariables(1, 10L, "Sales");
                    });
                    ctx.statement(it -> {
                        it.sql("merge into EMPLOYEE(ID, NAME, DEPARTMENT_ID) key(ID) values(?, ?, ?)");
                        it.batchVariables(0, 2L, "Linda", 1L);
                        it.batchVariables(1, 20L, "Raines", 1L);
                        it.batchVariables(2, 40L, "Oakes", 10L);
                    });
                    ctx.statement(it -> {
                        // Logical deletion is used by Employee, not physical deletion
                        it.sql(
                                "update EMPLOYEE set DELETED_UUID = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) " +
                                        "and DELETED_UUID is null"
                        );
                        it.batches(2);
                    });
                    ctx.entity(it -> {

                    });
                    ctx.entity(it -> {});
                }
        );
    }

    @Test
    public void upsertH2WithoutId() {
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
}
