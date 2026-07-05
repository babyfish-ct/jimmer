package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
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
                                        "when not matched then " +
                                        "--->insert(NAME, DELETED_MILLIS) values(tb_2_.NAME, tb_2_.DELETED_MILLIS)"
                        );
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
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
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.FETCHER);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
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
                                        "--->using( values(?, ?)) tb_2_(ID, NAME) " +
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
                                        "--->using( values(?, ?), (?, ?)) tb_2_(ID, NAME) " +
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
                                        "--->using( values(?, ?)) tb_2_(ID, NAME) " +
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
                                        "--->using( values(?, ?, ?)) tb_2_(ID, VERSION, NAME) " +
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
}
