package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
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
                    ctx.entity(it -> {});
                    ctx.entity(it -> {});
                }
        );
    }
}
