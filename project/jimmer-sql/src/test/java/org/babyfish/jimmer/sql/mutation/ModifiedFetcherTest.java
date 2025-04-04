package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentFetcher;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeFetcher;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentCompositeView;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiedFetcherTest extends AbstractMutationTest {

    @BeforeEach
    public void initialize() {
        resetIdentity(null, "DEPARTMENT");
        resetIdentity(null, "EMPLOYEE");
    }

    @Test
    public void testFetchMore() {
        Department department = Immutables.createDepartment(draft -> {
           draft.setName("Sales");
        });
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(department).execute(
                        con,
                        DepartmentFetcher.$.allScalarFields()
                                .employees(
                                        EmployeeFetcher.$.allScalarFields()
                                )
                ).getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when matched then " +
                                        "--->update set /* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
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
                    ctx.value(
                            "{\"id\":\"100\",\"name\":\"Sales\",\"employees\":[]}"
                    );
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
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(employee)
                        .execute(con, EmployeeFetcher.$.name())
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.value("{\"id\":\"100\",\"name\":\"Linda\"}");
                }
        );
    }

    @Test
    public void testFetchMoreByDTO() {
        List<Department> departments = Arrays.asList(
                Immutables.createDepartment(draft -> {
                    draft.setName("Sales");
                    draft.addIntoEmployees(employee -> {
                        employee.setName("Bob");
                        employee.setGender(Gender.MALE);
                    });
                }),
                Immutables.createDepartment(draft -> {
                    draft.setName("Market");
                })
        );
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(departments).execute(
                        con,
                        DepartmentCompositeView.class
                ).getViewItems()
                        .stream()
                        .map(BatchSaveResult.View.ViewItem::getModifiedView)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when matched then " +
                                        "--->update set /* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DEPARTMENT_ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID = ? and not (ID = any(?)) and DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "DepartmentCompositeView(" +
                                    "--->id=100, " +
                                    "--->employees=[" +
                                    "--->--->DepartmentCompositeView.TargetOf_employees(" +
                                    "--->--->--->id=100, " +
                                    "--->--->--->name=Bob" +
                                    "--->--->)" +
                                    "--->]" +
                                    "), " +
                                    "DepartmentCompositeView(" +
                                    "--->id=1, " +
                                    "--->employees=[" +
                                    "--->--->DepartmentCompositeView.TargetOf_employees(" +
                                    "--->--->--->id=1, " +
                                    "--->--->--->name=Sam" +
                                    "--->--->), " +
                                    "--->--->DepartmentCompositeView.TargetOf_employees(" +
                                    "--->--->--->id=2, " +
                                    "--->--->--->name=Jessica" +
                                    "--->--->)" +
                                    "--->]" +
                                    ")" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchLessByDTO() {
        List<Employee> employees = Arrays.asList(
                Immutables.createEmployee(draft -> {
                    draft.setName("Linda");
                    draft.setGender(Gender.FEMALE);
                    draft.setDepartmentId(1L);
                }),
                Immutables.createEmployee(draft -> {
                    draft.setName("Bob");
                    draft.setGender(Gender.MALE);
                    draft.setDepartmentId(1L);
                })
        );
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(employees)
                        .execute(con, EmployeeView.class)
                        .getViewItems().stream().map(
                                BatchSaveResult.View.ViewItem::getModifiedView
                        )
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "key(NAME, DELETED_MILLIS) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.value(
                            "[EmployeeView(id=100, name=Linda), " +
                                    "EmployeeView(id=101, name=Bob)]"
                    );
                }
        );
    }

    @Test
    public void testInsertIgnore() {
        List<Department> departments = Arrays.asList(
                Immutables.createDepartment(draft -> draft.setName("Market")),
                Immutables.createDepartment(draft -> draft.setName("Develop")),
                Immutables.createDepartment(draft -> draft.setName("Sales"))
        );
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(departments).setMode(SaveMode.INSERT_IF_ABSENT).execute(
                        con,
                        DepartmentFetcher.$
                                .allScalarFields()
                                .employees(
                                        EmployeeFetcher.$.allScalarFields()
                                )
                ).getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when matched then update set " +
                                        "--->/* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.ID = any(?) and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.DEPARTMENT_ID, tb_1_.ID, tb_1_.NAME, tb_1_.GENDER " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = any(?) and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"1\"," +
                                    "--->--->\"name\":\"Market\"," +
                                    "--->--->\"employees\":[" +
                                    "--->--->--->{\"id\":\"1\",\"name\":\"Sam\",\"gender\":\"MALE\"}," +
                                    "--->--->--->{\"id\":\"2\",\"name\":\"Jessica\",\"gender\":\"FEMALE\"}" +
                                    "--->--->]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"100\"," +
                                    "--->--->\"name\":\"Develop\"," +
                                    "--->--->\"employees\":[]" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"101\"," +
                                    "--->--->\"name\":\"Sales\"," +
                                    "--->--->\"employees\":[]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
