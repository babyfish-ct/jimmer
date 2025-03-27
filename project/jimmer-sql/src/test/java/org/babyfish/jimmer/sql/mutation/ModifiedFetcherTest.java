package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentFetcher;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeFetcher;
import org.junit.jupiter.api.Test;

public class ModifiedFetcherTest extends AbstractMutationTest {

    @Test
    public void testFetchMore() {
        Department department = Immutables.createDepartment(draft -> {
           draft.setName("Sales");
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(department).setFetcher(
                        DepartmentFetcher.$.allScalarFields()
                                .employees(
                                        EmployeeFetcher.$.allScalarFields()
                                )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT(NAME, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) " +
                                        "values(?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":\"100\",\"name\":\"Sales\",\"employees\":[]}");
                    });
                }
        );
    }

    @Test
    public void testFetchLess() {
        Employee employee = Immutables.createEmployee(draft -> {
            draft.setName("Linda");
            draft.setGender(Gender.FEMALE);
            draft.setDepartmentId(1L);
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(employee)
                        .setFetcher(EmployeeFetcher.$.name()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":\"100\",\"name\":\"Linda\"}");
                    });
                }
        );
    }
}
