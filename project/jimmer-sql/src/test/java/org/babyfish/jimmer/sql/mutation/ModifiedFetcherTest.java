package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentFetcher;
import org.babyfish.jimmer.sql.model.hr.DepartmentProps;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeFetcher;
import org.babyfish.jimmer.sql.model.hr.EmployeeProps;
import org.babyfish.jimmer.sql.model.hr.dto.DepartmentCompositeView;
import org.babyfish.jimmer.sql.model.hr.dto.EmployeeView;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiedFetcherTest extends AbstractMutationTest {

    @BeforeEach
    public void initialize() {
        resetIdentity(null, "DEPARTMENT");
        resetIdentity(null, "EMPLOYEE");
        resetIdentity(null, "SYS_USER");
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
                                "select ID, NAME, DELETED_MILLIS " +
                                        "from final table (" +
                                        "--->merge into DEPARTMENT tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "--->when matched then update set /* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "--->when not matched then insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
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
    public void testSaveReturningWorksWithRootUserFilter() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.addFilters(new Filter<BookProps>() {
                        @Override
                        public void filter(FilterArgs<BookProps> args) {
                            args.where(args.getTable().name().ne("Learning GraphQL protocol"));
                        }
                    });
                }).saveCommand(Immutables.createBook(draft -> {
                    draft.setId(Constants.learningGraphQLId1);
                    draft.setName("Learning GraphQL protocol");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(con, BookFetcher.$.name().edition())
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "\"name\":\"Learning GraphQL protocol\",\"edition\":1}"
                    );
                }
        );
    }

    @Test
    public void testPostSaveFetchIgnoresRootUserFilterOnly() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.addFilters(new Filter<DepartmentProps>() {
                        @Override
                        public void filter(FilterArgs<DepartmentProps> args) {
                            args.where(args.getTable().name().ne("Market"));
                        }
                    });
                    it.addFilters(new Filter<EmployeeProps>() {
                        @Override
                        public void filter(FilterArgs<EmployeeProps> args) {
                            args.where(args.getTable().name().ne("Sam"));
                        }
                    });
                }).saveCommand(Immutables.createDepartment(draft -> {
                    draft.setId(1L);
                    draft.setName("Market");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .setSaveReturningEnabled(false)
                        .execute(
                                con,
                                DepartmentFetcher.$
                                        .allScalarFields()
                                        .employees(EmployeeFetcher.$.allScalarFields())
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update DEPARTMENT " +
                                        "set NAME = ? " +
                                        "where ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
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
                                        "where tb_1_.DEPARTMENT_ID = ? and " +
                                        "tb_1_.DELETED_MILLIS = ? and " +
                                        "tb_1_.NAME <> ?"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"1\"," +
                                    "--->\"name\":\"Market\"," +
                                    "--->\"employees\":[" +
                                    "--->--->{\"id\":\"2\",\"name\":\"Jessica\",\"gender\":\"FEMALE\"}" +
                                    "--->]" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testSaveReturningRespectsIgnoredLogicalDeletedBehaviorWithUserFilter() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.addFilters(new Filter<EmployeeProps>() {
                        @Override
                        public void filter(FilterArgs<EmployeeProps> args) {
                            args.where(args.getTable().name().ne("Samuel"));
                        }
                    });
                }).filters(it -> {
                    it.setBehavior(Employee.class, LogicalDeletedBehavior.IGNORED);
                }).saveCommand(Immutables.createEmployee(draft -> {
                    draft.setId(3L);
                    draft.setName("Samuel");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                EmployeeFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, GENDER " +
                                        "from final table (" +
                                        "--->merge into EMPLOYEE tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"3\"," +
                                    "--->\"name\":\"Samuel\"," +
                                    "--->\"gender\":\"MALE\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testPostSaveFetchRespectsIgnoredLogicalDeletedBehaviorWithUserFilter() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.addFilters(new Filter<EmployeeProps>() {
                        @Override
                        public void filter(FilterArgs<EmployeeProps> args) {
                            args.where(args.getTable().name().ne("Samuel"));
                        }
                    });
                }).filters(it -> {
                    it.setBehavior(Employee.class, LogicalDeletedBehavior.IGNORED);
                }).saveCommand(Immutables.createEmployee(draft -> {
                    draft.setId(3L);
                    draft.setName("Samuel");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .setSaveReturningEnabled(false)
                        .execute(
                                con,
                                EmployeeFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set NAME = ? " +
                                        "where ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"3\"," +
                                    "--->\"name\":\"Samuel\"," +
                                    "--->\"gender\":\"MALE\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testSaveReturningByKeyDoesNotAffectLogicalDeletedRows() {
        connectAndExpect(
                con -> {
                    Employee modifiedEntity = getSqlClient(it -> {
                        it.setDialect(new H2Dialect());
                    }).saveCommand(Immutables.createEmployee(draft -> {
                        draft.setName("Sam");
                        draft.setGender(Gender.FEMALE);
                    })).setMode(SaveMode.UPDATE_ONLY)
                            .execute(
                                    con,
                                    EmployeeFetcher.$.allScalarFields()
                            )
                            .getModifiedEntity();
                    return modifiedEntity + "; deletedGender=" + employeeGender(con, 3L);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, GENDER, DELETED_MILLIS " +
                                        "from final table (" +
                                        "--->merge into EMPLOYEE tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(NAME, GENDER) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = ? " +
                                        "--->when matched then update set GENDER = tb_2_.GENDER" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"1\"," +
                                    "--->\"name\":\"Sam\"," +
                                    "--->\"gender\":\"FEMALE\"" +
                                    "}; deletedGender=M"
                    );
                }
        );
    }

    @Test
    public void testSaveReturningCanBeDisabledGlobally() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setDefaultSaveReturningEnabled(false);
                }).saveCommand(Immutables.createBook(draft -> {
                    draft.setId(Constants.learningGraphQLId1);
                    draft.setName("Learning GraphQL protocol");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(con, BookFetcher.$.name().edition())
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK " +
                                        "set NAME = ? " +
                                        "where ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "\"name\":\"Learning GraphQL protocol\"," +
                                    "\"edition\":1}"
                    );
                }
        );
    }

    @Test
    public void testSaveReturningByUserOptimisticLock() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveCommand(Immutables.createBook(draft -> {
                            draft.setId(Constants.graphQLInActionId3);
                            draft.setPrice(new BigDecimal("79.00"));
                        }))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setOptimisticLock(
                                BookTable.class,
                                (table, it) -> table.price().ge(it.newValue(BookProps.PRICE))
                        )
                        .execute(
                                con,
                                BookFetcher.$.price().edition()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, PRICE, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, PRICE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.PRICE >= tb_2_.PRICE " +
                                        "--->when matched then update set PRICE = tb_2_.PRICE" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"price\":79.00," +
                                    "--->\"edition\":3" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testSaveReturningByUserOptimisticLockFailed() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .saveCommand(Immutables.createBook(draft -> {
                            draft.setId(Constants.graphQLInActionId3);
                            draft.setPrice(new BigDecimal("81.00"));
                        }))
                        .setMode(SaveMode.UPDATE_ONLY)
                        .setOptimisticLock(
                                BookTable.class,
                                (table, it) -> table.price().ge(it.newValue(BookProps.PRICE))
                        )
                        .execute(
                                con,
                                BookFetcher.$.price().edition()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, PRICE, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, PRICE) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.PRICE >= tb_2_.PRICE " +
                                        "--->when matched then update set PRICE = tb_2_.PRICE" +
                                        ")"
                        );
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.OptimisticLockError.class);
                        it.message(
                                "Save error caused by the path: \"<root>\": " +
                                        "Cannot update the entity whose type is " +
                                        "\"org.babyfish.jimmer.sql.model.Book\" and id is " +
                                        "\"780bdf07-05af-48bf-9be9-f8c65236fecc\" " +
                                        "because of optimistic lock error"
                        );
                    });
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
                                "select ID, DELETED_MILLIS, NAME " +
                                        "from final table (" +
                                        "--->merge into DEPARTMENT tb_1_ " +
                                        "--->using(values(?, ?), (?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "--->when matched then update set /* fake update to return all ids */ DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "--->when not matched then insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
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
                                "select ID, NAME, DELETED_MILLIS " +
                                        "from final table (" +
                                        "--->merge into DEPARTMENT tb_1_ " +
                                        "--->using(values(?, ?), (?, ?), (?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "--->when not matched then insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID " +
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
                                    "--->--->\"name\":\"Market\"" +
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

    @Test
    public void testIssue982ByQueryReturnDraft() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createEmployee(draft -> {
                    draft.setId(1L);
                    draft.setName("Jhon");
                })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                EmployeeFetcher.$.name().gender()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, GENDER, DELETED_MILLIS " +
                                        "from final table (" +
                                        "--->merge into EMPLOYEE tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.value("{\"id\":\"1\",\"name\":\"Jhon\",\"gender\":\"MALE\"}");
                }
        );
    }

    @Test
    public void testBatchUpdateByQueryReturnDraft() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(
                                Arrays.asList(
                                        Immutables.createBook(draft -> {
                                            draft.setId(Constants.learningGraphQLId1);
                                            draft.setName("Learning GraphQL protocol");
                                        }),
                                        Immutables.createBook(draft -> {
                                            draft.setId(Constants.effectiveTypeScriptId1);
                                            draft.setName("Effective TypeScript protocol");
                                        })
                                )
                        ).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookFetcher.$.name().edition()
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?), (?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "--->{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "\"name\":\"Learning GraphQL protocol\",\"edition\":1}, " +
                                    "--->{\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                    "\"name\":\"Effective TypeScript protocol\",\"edition\":1}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testUpdateByQueryReturnDraftWithResidualAssociationFetcher() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createBook(draft -> {
                            draft.setId(Constants.learningGraphQLId1);
                            draft.setName("Learning GraphQL protocol");
                        })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookFetcher.$
                                        .name()
                                        .edition()
                                        .store(BookStoreFetcher.$.name())
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->\"name\":\"Learning GraphQL protocol\"," +
                                    "--->\"edition\":1," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"" +
                                    "--->}" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testBatchUpdateByQueryReturnDraftWithResidualAssociationFetcher() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(
                                Arrays.asList(
                                        Immutables.createBook(draft -> {
                                            draft.setId(Constants.learningGraphQLId1);
                                            draft.setName("Learning GraphQL protocol");
                                        }),
                                        Immutables.createBook(draft -> {
                                            draft.setId(Constants.effectiveTypeScriptId1);
                                            draft.setName("Effective TypeScript protocol");
                                        })
                                )
                        ).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookFetcher.$
                                        .name()
                                        .edition()
                                        .store(BookStoreFetcher.$.name())
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?), (?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = any(?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL protocol\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}, {" +
                                    "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                    "--->--->\"name\":\"Effective TypeScript protocol\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIssue982ByQueryImmutable() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createBook(draft -> {
                            draft.setId(Constants.learningGraphQLId1);
                            draft.setName("Learning GraphQL protocol");
                        })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookFetcher.$.name().edition()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "\"name\":\"Learning GraphQL protocol\"," +
                                    "\"edition\":1}"
                    );
                }
        );
    }

    @Test
    public void testUpdateByQueryUsesKnownVersionWithoutReturning() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createBookStore(draft -> {
                            draft.setId(Constants.oreillyId);
                            draft.setName("O'REILLY");
                            draft.setVersion(0);
                        })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookStoreFetcher.$.name().version()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set NAME = ?, VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->\"name\":\"O'REILLY\"," +
                                    "--->\"version\":1" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpdateByQueryReturnDraftWithVersionAndMissingField() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createBookStore(draft -> {
                            draft.setId(Constants.oreillyId);
                            draft.setName("O'REILLY");
                            draft.setVersion(0);
                        })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                BookStoreFetcher.$.name().version().website()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, VERSION, NAME, WEBSITE " +
                                        "from final table (" +
                                        "--->merge into BOOK_STORE tb_1_ " +
                                        "--->using(values(?, ?, ?)) tb_2_(ID, VERSION, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID and tb_1_.VERSION = tb_2_.VERSION " +
                                        "--->when matched then update set NAME = tb_2_.NAME, VERSION = tb_1_.VERSION + 1" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->\"name\":\"O'REILLY\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":1" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testInsertOnlyReturnDraftWithGeneratedId() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createSysUser(draft -> {
                            draft.setAccount("linda");
                            draft.setEmail("linda@jimmer.org");
                            draft.setArea("dev");
                            draft.setNickName("Linda");
                        })).setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                SysUserFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DESCRIPTION " +
                                        "from final table (" +
                                        "--->insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->values(?, ?, ?, ?)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":100," +
                                    "--->\"account\":\"linda\"," +
                                    "--->\"email\":\"linda@jimmer.org\"," +
                                    "--->\"area\":\"dev\"," +
                                    "--->\"nickName\":\"Linda\"," +
                                    "--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testInsertOnlyReturnDraftWithGeneratedIdWithoutDatabaseDefaultField() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createSysUser(draft -> {
                            draft.setAccount("linda");
                            draft.setEmail("linda@jimmer.org");
                            draft.setArea("dev");
                            draft.setNickName("Linda");
                        })).setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                SysUserFetcher.$
                                        .account()
                                        .email()
                                        .area()
                                        .nickName()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?)"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":100," +
                                    "--->\"account\":\"linda\"," +
                                    "--->\"email\":\"linda@jimmer.org\"," +
                                    "--->\"area\":\"dev\"," +
                                    "--->\"nickName\":\"Linda\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testBatchInsertOnlyReturnDraftWithGeneratedId() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(Arrays.asList(
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("linda");
                                    draft.setEmail("linda@jimmer.org");
                                    draft.setArea("dev");
                                    draft.setNickName("Linda");
                                }),
                                Immutables.createSysUser(draft -> {
                                    draft.setAccount("bob");
                                    draft.setEmail("bob@jimmer.org");
                                    draft.setArea("dev");
                                    draft.setNickName("Bob");
                                })
                        ))
                        .setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                SysUserFetcher.$.allScalarFields()
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, DESCRIPTION " +
                                        "from final table (" +
                                        "--->insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->values(?, ?, ?, ?), (?, ?, ?, ?)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":100," +
                                    "--->--->\"account\":\"linda\"," +
                                    "--->--->\"email\":\"linda@jimmer.org\"," +
                                    "--->--->\"area\":\"dev\"," +
                                    "--->--->\"nickName\":\"Linda\"," +
                                    "--->--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "--->}, " +
                                    "--->{" +
                                    "--->--->\"id\":101," +
                                    "--->--->\"account\":\"bob\"," +
                                    "--->--->\"email\":\"bob@jimmer.org\"," +
                                    "--->--->\"area\":\"dev\"," +
                                    "--->--->\"nickName\":\"Bob\"," +
                                    "--->--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testInsertOnlyReturnDraftWithGeneratedIdByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE, "SYS_USER");

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createSysUser(draft -> {
                            draft.setAccount("pg-linda");
                            draft.setEmail("pg-linda@jimmer.org");
                            draft.setArea("pg-dev");
                            draft.setNickName("PgLinda");
                        })).setMode(SaveMode.INSERT_ONLY)
                        .execute(
                                con,
                                SysUserFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?) returning ID, DESCRIPTION"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":100," +
                                    "--->\"account\":\"pg-linda\"," +
                                    "--->\"email\":\"pg-linda@jimmer.org\"," +
                                    "--->\"area\":\"pg-dev\"," +
                                    "--->\"nickName\":\"PgLinda\"," +
                                    "--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpdateOnlyReturnDraftByPostgres() {
        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .saveCommand(Immutables.createSysUser(draft -> {
                            draft.setId(1L);
                            draft.setAccount("pg-tom");
                        })).setMode(SaveMode.UPDATE_ONLY)
                        .execute(
                                con,
                                SysUserFetcher.$.account().description()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update SYS_USER tb_1_ " +
                                        "set ACCOUNT = tb_2_.ACCOUNT " +
                                        "from (values(?, ?)) tb_2_(ID, ACCOUNT) " +
                                        "where tb_1_.ID = tb_2_.ID " +
                                        "returning tb_1_.ID, tb_1_.ACCOUNT, tb_1_.DESCRIPTION"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":1," +
                                    "--->\"account\":\"pg-tom\"," +
                                    "--->\"description\":\"description_001\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertReturnDraftByPostgres() {
        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .saveCommand(Immutables.createSysUser(draft -> {
                            draft.setId(1L);
                            draft.setAccount("pg-upsert");
                            draft.setEmail("pg-upsert@jimmer.org");
                            draft.setArea("pg-dev");
                            draft.setNickName("PgUpsert");
                        })).setMode(SaveMode.UPSERT)
                        .execute(
                                con,
                                SysUserFetcher.$.account().description()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ID, ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?, ?) " +
                                        "on conflict(ID) do update set " +
                                        "ACCOUNT = excluded.ACCOUNT, " +
                                        "EMAIL = excluded.EMAIL, " +
                                        "AREA = excluded.AREA, " +
                                        "NICK_NAME = excluded.NICK_NAME " +
                                        "returning ID, ACCOUNT, DESCRIPTION"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":1," +
                                    "--->\"account\":\"pg-upsert\"," +
                                    "--->\"description\":\"description_001\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertReturnDraftByUserOptimisticLockByPostgres() {
        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .saveCommand(Immutables.createSysUser(draft -> {
                            draft.setId(1L);
                            draft.setAccount("pg-upsert-lock");
                            draft.setEmail("pg-upsert-lock@jimmer.org");
                            draft.setArea("pg-dev");
                            draft.setNickName("PgUpsertLock");
                        })).setMode(SaveMode.UPSERT)
                        .setOptimisticLock(
                                SysUserTable.class,
                                (table, it) -> table.account().ne(it.newString(SysUserProps.ACCOUNT))
                        )
                        .execute(
                                con,
                                SysUserFetcher.$.account().description()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ID, ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?, ?) " +
                                        "on conflict(ID) do update set " +
                                        "ACCOUNT = excluded.ACCOUNT, " +
                                        "EMAIL = excluded.EMAIL, " +
                                        "AREA = excluded.AREA, " +
                                        "NICK_NAME = excluded.NICK_NAME " +
                                        "where SYS_USER.ACCOUNT <> excluded.ACCOUNT " +
                                        "returning ID, ACCOUNT, DESCRIPTION"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":1," +
                                    "--->\"account\":\"pg-upsert-lock\"," +
                                    "--->\"description\":\"description_001\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertByUserOptimisticLockWithoutReturningByPostgres() {
        NativeDatabases.assumeNativeDatabase();

        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> getSqlClient(it -> it.setDialect(new PostgresDialect()))
                        .saveCommand(Immutables.createSysUser(draft -> {
                            draft.setId(1L);
                            draft.setAccount("pg-upsert-lock-without-returning");
                            draft.setEmail("pg-upsert-lock-without-returning@jimmer.org");
                            draft.setArea("pg-dev");
                            draft.setNickName("PgUpsertLockWithoutReturning");
                        })).setMode(SaveMode.UPSERT)
                        .setSaveReturningEnabled(false)
                        .setOptimisticLock(
                                SysUserTable.class,
                                (table, it) -> table.account().ne(it.newString(SysUserProps.ACCOUNT))
                        )
                        .execute(
                                con,
                                SysUserFetcher.$.account().description()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ID, ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?, ?) " +
                                        "on conflict(ID) do update set " +
                                        "ACCOUNT = excluded.ACCOUNT, " +
                                        "EMAIL = excluded.EMAIL, " +
                                        "AREA = excluded.AREA, " +
                                        "NICK_NAME = excluded.NICK_NAME " +
                                        "where SYS_USER.ACCOUNT <> ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.DESCRIPTION " +
                                        "from SYS_USER tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":1," +
                                    "--->\"account\":\"pg-upsert-lock-without-returning\"," +
                                    "--->\"email\":\"pg-upsert-lock-without-returning@jimmer.org\"," +
                                    "--->\"area\":\"pg-dev\"," +
                                    "--->\"nickName\":\"PgUpsertLockWithoutReturning\"," +
                                    "--->\"description\":\"description_001\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testUpsertReturnDraftWithMissingField() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createBook(draft -> {
                            draft.setId(Constants.learningGraphQLId1);
                            draft.setName("Learning GraphQL protocol");
                        })).setMode(SaveMode.UPSERT)
                        .execute(
                                con,
                                BookFetcher.$.name().edition()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, NAME, EDITION " +
                                        "from final table (" +
                                        "--->merge into BOOK tb_1_ " +
                                        "--->using(values(?, ?)) tb_2_(ID, NAME) " +
                                        "--->on tb_1_.ID = tb_2_.ID " +
                                        "--->when matched then update set NAME = tb_2_.NAME " +
                                        "--->when not matched then insert(ID, NAME) values(tb_2_.ID, tb_2_.NAME)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "\"name\":\"Learning GraphQL protocol\"," +
                                    "\"edition\":1}"
                    );
                }
        );
    }

    @Test
    public void testInsertIfAbsentReturnDraftForInsertedRows() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(Immutables.createSysUser(draft -> {
                            draft.setAccount("new-account");
                            draft.setEmail("new-account@jimmer.org");
                            draft.setArea("dev");
                            draft.setNickName("Newbie");
                        })).setMode(SaveMode.INSERT_IF_ABSENT)
                        .execute(
                                con,
                                SysUserFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into SYS_USER tb_1_ " +
                                        "--->using(values(?, ?, ?, ?)) tb_2_(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->on tb_1_.AREA = tb_2_.AREA and tb_1_.NICK_NAME = tb_2_.NICK_NAME " +
                                        "--->when not matched then insert(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->values(tb_2_.ACCOUNT, tb_2_.EMAIL, tb_2_.AREA, tb_2_.NICK_NAME)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":100," +
                                    "--->\"account\":\"new-account\"," +
                                    "--->\"email\":\"new-account@jimmer.org\"," +
                                    "--->\"area\":\"dev\"," +
                                    "--->\"nickName\":\"Newbie\"," +
                                    "--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testInsertIfAbsentReturningDoesNotMaterializeConflictRows() {
        connectAndExpect(
                con -> {
                    SimpleSaveResult<SysUser> result = getSqlClient(it -> {
                        it.setDialect(new H2Dialect());
                        it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    }).saveCommand(Immutables.createSysUser(draft -> {
                                draft.setAccount("conflict-account");
                                draft.setEmail("conflict-email@jimmer.org");
                                draft.setArea("north");
                                draft.setNickName("Tom");
                            })).setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(
                                    con,
                                    SysUserFetcher.$.allScalarFields()
                            );
                    return result.getTotalAffectedRowCount() +
                            "; " +
                            result.getAffectedRowCount(SysUser.class) +
                            "; " +
                            (result.getOriginalEntity() == result.getModifiedEntity()) +
                            "; " +
                            result.getModifiedEntity();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into SYS_USER tb_1_ " +
                                        "--->using(values(?, ?, ?, ?)) tb_2_(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->on tb_1_.AREA = tb_2_.AREA and tb_1_.NICK_NAME = tb_2_.NICK_NAME " +
                                        "--->when not matched then insert(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->values(tb_2_.ACCOUNT, tb_2_.EMAIL, tb_2_.AREA, tb_2_.NICK_NAME)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "0; 0; true; " +
                                    "{" +
                                    "--->\"account\":\"conflict-account\"," +
                                    "--->\"email\":\"conflict-email@jimmer.org\"," +
                                    "--->\"area\":\"north\"," +
                                    "--->\"nickName\":\"Tom\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testBatchInsertIfAbsentReturningDoesNotMaterializeConflictRows() {
        connectAndExpect(
                con -> {
                    BatchSaveResult<SysUser> result = getSqlClient(it -> {
                        it.setDialect(new H2Dialect());
                        it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    }).saveEntitiesCommand(Arrays.asList(
                            Immutables.createSysUser(draft -> {
                                draft.setAccount("batch-new-account");
                                draft.setEmail("batch-new-account@jimmer.org");
                                draft.setArea("dev");
                                draft.setNickName("BatchNewbie");
                            }),
                            Immutables.createSysUser(draft -> {
                                draft.setAccount("batch-conflict-account");
                                draft.setEmail("batch-conflict-email@jimmer.org");
                                draft.setArea("north");
                                draft.setNickName("Tom");
                            })
                    )).setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(
                                    con,
                                    SysUserFetcher.$.allScalarFields()
                            );
                    return result.getTotalAffectedRowCount() +
                            "; " +
                            (result.getItems().get(0).getOriginalEntity() ==
                                    result.getItems().get(0).getModifiedEntity()) +
                            "; " +
                            result.getItems().get(0).getModifiedEntity() +
                            "; " +
                            (result.getItems().get(1).getOriginalEntity() ==
                                    result.getItems().get(1).getModifiedEntity()) +
                            "; " +
                            result.getItems().get(1).getModifiedEntity();
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, ACCOUNT, EMAIL, AREA, NICK_NAME, DESCRIPTION " +
                                        "from final table (" +
                                        "--->merge into SYS_USER tb_1_ " +
                                        "--->using(values(?, ?, ?, ?), (?, ?, ?, ?)) tb_2_(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->on tb_1_.AREA = tb_2_.AREA and tb_1_.NICK_NAME = tb_2_.NICK_NAME " +
                                        "--->when not matched then insert(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "--->values(tb_2_.ACCOUNT, tb_2_.EMAIL, tb_2_.AREA, tb_2_.NICK_NAME)" +
                                        ")"
                        );
                    });
                    ctx.value(
                            "1; false; " +
                                    "{" +
                                    "--->\"id\":100," +
                                    "--->\"account\":\"batch-new-account\"," +
                                    "--->\"email\":\"batch-new-account@jimmer.org\"," +
                                    "--->\"area\":\"dev\"," +
                                    "--->\"nickName\":\"BatchNewbie\"," +
                                    "--->\"description\":\"DEFAULT_DESCRIPTION\"" +
                                    "}; true; " +
                                    "{" +
                                    "--->\"account\":\"batch-conflict-account\"," +
                                    "--->\"email\":\"batch-conflict-email@jimmer.org\"," +
                                    "--->\"area\":\"north\"," +
                                    "--->\"nickName\":\"Tom\"" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void testIssue1000BySimple() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveCommand(
                        Immutables.createDepartment(draft -> {
                            draft.setName("Market");
                        })
                )
                        .setMode(SaveMode.INSERT_IF_ABSENT)
                        .execute(
                                con,
                                DepartmentFetcher.$.allScalarFields()
                        )
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":\"1\",\"name\":\"Market\"" +
                                    "}");
                }
        );
    }

    @Test
    public void testIssue1000ByBatch() {
        connectAndExpect(
                con -> getSqlClient(it -> {
                    it.setDialect(new H2Dialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createDepartment(draft -> {
                                    draft.setName("Market");
                                }),
                                Immutables.createDepartment(draft -> {
                                    draft.setName("Sales");
                                })
                        )
                )
                        .setMode(SaveMode.INSERT_IF_ABSENT)
                        .execute(
                                con,
                                DepartmentFetcher.$.allScalarFields()
                        )
                        .getItems()
                        .stream()
                        .map(BatchSaveResult.Item::getModifiedEntity)
                        .collect(Collectors.toList()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into DEPARTMENT tb_1_ " +
                                        "using(values(?, ?)) tb_2_(NAME, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "[" +
                                    "--->{\"id\":\"1\",\"name\":\"Market\"}, " +
                                    "--->{\"id\":\"100\",\"name\":\"Sales\"}" +
                                    "]");
                }
        );
    }

    private static String employeeGender(Connection con, long id) {
        try (Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("select gender from employee where id = " + id)) {
                if (!rs.next()) {
                    throw new IllegalStateException("Cannot find employee " + id);
                }
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
