package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.DraftPreProcessor;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeDraft;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DraftHandlerTest extends AbstractMutationTest {

    private JSqlClient sqlClient1 = getSqlClient(it -> {
        it.addDraftInterceptor(
                new DraftInterceptor<Book, BookDraft>() {

                    @Override
                    public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {

                    }

                    @Override
                    public Collection<TypedProp<Book, ?>> dependencies() {
                        return Collections.singleton(BookProps.EDITION);
                    }
                }
        );
    });

    private JSqlClient sqlClient2 = getSqlClient(it -> {
        it.addDraftInterceptor(
                new DraftInterceptor<Book, BookDraft>() {

                    @Override
                    public void beforeSave(@NotNull BookDraft draft, @Nullable Book original) {
                        if (original != null && !ImmutableObjects.isLoaded(draft, BookProps.PRICE)) {
                            draft.setPrice(original.price().add(new BigDecimal("7.77")));
                        }
                    }

                    @Override
                    public Collection<TypedProp<Book, ?>> dependencies() {
                        return Collections.singleton(BookProps.PRICE);
                    }
                }
        );
    });

    @Test
    public void testKeyOnlyDraftHandler() {
        executeAndExpectResult(
                sqlClient1
                        .getEntities()
                        .saveCommand(
                                BookDraft.$.produce(book -> {
                                    book.setId(Constants.graphQLInActionId3);
                                    book.setName("GraphQL in Action+");
                                })
                        ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(Constants.graphQLInActionId3);
                        it.queryReason(QueryReason.INTERCEPTOR);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set NAME = ? where ID = ?");
                        it.variables("GraphQL in Action+", Constants.graphQLInActionId3);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                "--->\"name\":\"GraphQL in Action+\"" +
                                "}");
                        it.modified("{" +
                                "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                "--->\"name\":\"GraphQL in Action+\"" +
                                "}");
                    });
                }
        );
    }

    @Test
    public void testNonKeyOnlyDraftHandler() {
        executeAndExpectResult(
                sqlClient2
                        .getEntities()
                        .saveCommand(
                                BookDraft.$.produce(book -> {
                                    book.setId(Constants.graphQLInActionId3);
                                    book.setName("GraphQL in Action+");
                                })
                        ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                        "from BOOK tb_1_ where tb_1_.ID = ?"
                        );
                        it.variables(Constants.graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set NAME = ?, PRICE = ? where ID = ?");
                        it.variables("GraphQL in Action+", new BigDecimal("87.77"), Constants.graphQLInActionId3);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->\"name\":\"GraphQL in Action+\"" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->\"name\":\"GraphQL in Action+\"," +
                                        "--->\"price\":87.77" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testIssue882ByH2() {
        DraftPreProcessor<EmployeeDraft> processor = new DraftPreProcessor<EmployeeDraft>() {
            @Override
            public void beforeSave(@NotNull EmployeeDraft draft) {
                draft.setGender(Gender.MALE);
            }
        };
        EmployeeTable table = EmployeeTable.$;
        connectAndExpect(
                con -> {
                    int rowCount = getSqlClient(it -> {
                        it.addDraftPreProcessor(processor);
                        it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    })
                            .saveEntitiesCommand(
                                    Arrays.asList(
                                            Immutables.createEmployee(draft -> {
                                                draft.setName("Jessica");
                                                draft.setDepartmentId(1L);
                                            }),
                                            Immutables.createEmployee(draft -> {
                                                draft.setName("Tim");
                                                draft.setDepartmentId(1L);
                                            })
                                    )
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    Assertions.assertEquals(1, rowCount);
                    return getSqlClient()
                            .createQuery(table)
                            .where(table.departmentId().eq(1L))
                            .select(table)
                            .execute(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into EMPLOYEE tb_1_ " +
                                        "using(values(?, ?, ?, ?)) tb_2_(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "--->on tb_1_.NAME = tb_2_.NAME and tb_1_.DELETED_MILLIS = tb_2_.DELETED_MILLIS " +
                                        "when not matched then " +
                                        "--->insert(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "--->values(tb_2_.NAME, tb_2_.GENDER, tb_2_.DEPARTMENT_ID, tb_2_.DELETED_MILLIS)"
                        );
                        it.batchVariables(0, "Jessica", "M", 1L, 0L);
                        it.batchVariables(1, "Tim", "M", 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER, tb_1_.DELETED_MILLIS, tb_1_.DEPARTMENT_ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "[{\"id\":\"1\",\"name\":\"Sam\",\"gender\":\"MALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}, " +
                                    "{\"id\":\"2\",\"name\":\"Jessica\",\"gender\":\"FEMALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}, " +
                                    "{\"id\":\"100\",\"name\":\"Tim\",\"gender\":\"MALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}]"
                    );
                }
        );
    }

    @Test
    public void testIssue882ByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        DraftPreProcessor<EmployeeDraft> processor = new DraftPreProcessor<EmployeeDraft>() {
            @Override
            public void beforeSave(@NotNull EmployeeDraft draft) {
                draft.setGender(Gender.MALE);
            }
        };

        jdbc(con -> resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE, "EMPLOYEE"));
        EmployeeTable table = EmployeeTable.$;
        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> {
                    int rowCount = getSqlClient(it -> {
                        it.addDraftPreProcessor(processor);
                        it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                        it.setDialect(new PostgresDialect());
                    })
                            .saveEntitiesCommand(
                                    Arrays.asList(
                                            Immutables.createEmployee(draft -> {
                                                draft.setName("Jessica");
                                                draft.setDepartmentId(1L);
                                            }),
                                            Immutables.createEmployee(draft -> {
                                                draft.setName("Tim");
                                                draft.setDepartmentId(1L);
                                            })
                                    )
                            )
                            .setMode(SaveMode.INSERT_IF_ABSENT)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    Assertions.assertEquals(1, rowCount);
                    return getSqlClient()
                            .createQuery(table)
                            .where(table.departmentId().eq(1L))
                            .select(table)
                            .execute(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DEPARTMENT_ID, DELETED_MILLIS) " +
                                        "values(?, ?, ?, ?) " +
                                        "on conflict(NAME, DELETED_MILLIS) do nothing returning ID"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.GENDER, tb_1_.DELETED_MILLIS, tb_1_.DEPARTMENT_ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.value(
                            "[{\"id\":\"1\",\"name\":\"Sam\",\"gender\":\"MALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}, " +
                                    "{\"id\":\"2\",\"name\":\"Jessica\",\"gender\":\"FEMALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}, " +
                                    "{\"id\":\"101\",\"name\":\"Tim\",\"gender\":\"MALE\",\"deletedMillis\":0,\"department\":{\"id\":\"1\"}}]"
                    );
                }
        );
    }
}
