package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.di.LogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.sql.meta.LogicalDeletedUUIDGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeProps;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadata;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

public class DeleteTest extends AbstractMutationTest {

    @Test
    public void testDeleteBookStore() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID from BOOK where STORE_ID = ?");
                        it.variables(manningId);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message(
                                "Cannot delete entities whose type are \"org.babyfish.jimmer.sql.model.BookStore\" " +
                                        "because there are some child entities whose type are \"org.babyfish.jimmer.sql.model.Book\", " +
                                        "these child entities use the association property \"org.babyfish.jimmer.sql.model.Book.store\" " +
                                        "to reference current entities."
                        );
                    });
                }
        );
    }

    @Test
    public void testDeleteBookStoreOnDissociateSetNull() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setDissociateAction(
                            BookProps.STORE,
                            DissociateAction.SET_NULL
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where STORE_ID = ?");
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID = ?");
                        it.variables(manningId);
                    });
                    ctx
                            .totalRowCount(4)
                            .rowCount(AffectedTable.of(Book.class), 3)
                            .rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testDeleteBookStoreOnDissociateDelete() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setDissociateAction(
                            BookProps.STORE,
                            DissociateAction.DELETE
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID from BOOK where STORE_ID = ?");
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in (?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID = ?");
                        it.variables(manningId);
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 3);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 3);
                }
        );
    }

    @Test
    public void testBook() {
        UUID nonExistingId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().batchDeleteCommand(
                        Book.class,
                        Arrays.asList(
                            learningGraphQLId1,
                            learningGraphQLId2,
                            nonExistingId
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in (?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 4);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                }
        );
    }

    @Test
    public void testDeleteAuthor() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Author.class,
                        alexId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR_COUNTRY_MAPPING where AUTHOR_ID = ?");
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ?");
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR where ID = ?");
                        it.variables(alexId);
                    });
                    ctx.totalRowCount(5);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.COUNTRY), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 3);
                }
        );
    }

    @Test
    public void testDeleteAdministrator() {
        executeAndExpectResult(
                getSqlClient().getEntities()
                        .deleteCommand(Administrator.class, 1L)
                        .setMode(DeleteMode.PHYSICAL),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from ADMINISTRATOR_ROLE_MAPPING where ADMINISTRATOR_ID = ?");
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select ID " +
                                        "from ADMINISTRATOR_METADATA " +
                                        "where ADMINISTRATOR_ID = ?"
                        );
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ADMINISTRATOR_METADATA where ID = ?");
                        it.variables(10L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ADMINISTRATOR where ID = ?");
                    });
                }
        );
    }

    @Test
    public void deleteTree() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in (?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in (?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in (?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in (?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID = ?");
                    });
                    ctx.totalRowCount(24);
                }
        );
    }

    @Test
    public void testLogicalDelete() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        AdministratorMetadata.class,
                        10L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update ADMINISTRATOR_METADATA set DELETED = ? where ID = ?");
                        it.variables(true, 10L);
                    });
                    ctx.totalRowCount(1);
                }
        );
    }

    @Test
    public void testCascadeLogicalDeleteWithLax() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Department.class,
                        1L
                ).setDissociateAction(EmployeeProps.DEPARTMENT, DissociateAction.LAX),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update DEPARTMENT set DELETED_TIME = ? where ID = ?");
                    });
                    ctx.totalRowCount(1);
                }
        );
    }

    @Test
    public void testCascadeLogicalDeleteWithCheck() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Department.class,
                        1L
                ).setDissociateAction(EmployeeProps.DEPARTMENT, DissociateAction.CHECK),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_UUID, tb_1_.DEPARTMENT_ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_UUID is null"
                        );
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Cannot delete entities whose type are \"org.babyfish.jimmer.sql.model.hr.Department\" " +
                                        "because there are some child entities whose type are \"org.babyfish.jimmer.sql.model.hr.Employee\", " +
                                        "these child entities use the association property \"org.babyfish.jimmer.sql.model.hr.Employee.department\" " +
                                        "to reference current entities."
                        );
                    });
                }
        );
    }

    @Test
    public void testCascadeLogicalDeleteWithSetNull() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Department.class,
                        1L
                ).setDissociateAction(
                        EmployeeProps.DEPARTMENT,
                        DissociateAction.SET_NULL
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE tb_1_ " +
                                        "set DEPARTMENT_ID = null " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_UUID is null"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update DEPARTMENT set DELETED_TIME = ? where ID = ?");
                    });
                    ctx.totalRowCount(3);
                }
        );
    }

    @Test
    public void testCascadeForceDeleteWithDelete() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Department.class,
                        1L,
                        DeleteMode.PHYSICAL
                ).setDissociateAction(
                        EmployeeProps.DEPARTMENT,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID from EMPLOYEE where DEPARTMENT_ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from EMPLOYEE where ID in (?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from DEPARTMENT where ID = ?");
                    });
                    ctx.totalRowCount(3);
                }
        );
    }

    @Test
    public void testCascadeForceWithDelete() {
        Assertions.assertSame(
                LogicalDeletedUUIDGenerator.class,
                ImmutableType.get(Employee.class).getLogicalDeletedInfo().getGeneratorType()
        );
        executeAndExpectResult(
                getSqlClient(cfg -> {
                    cfg.setLogicalDeletedValueGeneratorProvider(
                            new LogicalDeletedValueGeneratorProvider() {
                                @Override
                                public LogicalDeletedValueGenerator<?> get(Class<LogicalDeletedValueGenerator<?>> type, JSqlClient sqlClient) throws Exception {
                                    return new LogicalDeletedValueGenerator<UUID>() {
                                        @Override
                                        public UUID generate() {
                                            return UUID.fromString("11111111-1111-1111-1111-111111111111");
                                        }
                                    };
                                }
                            }
                    );
                }).getEntities().deleteCommand(
                        Department.class,
                        1L
                ).setDissociateAction(
                        EmployeeProps.DEPARTMENT,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_UUID, tb_1_.DEPARTMENT_ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? and " +
                                        "tb_1_.DELETED_UUID is null"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update EMPLOYEE set DELETED_UUID = ? where ID in (?, ?)");
                        it.variables(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql("update DEPARTMENT set DELETED_TIME = ? where ID = ?");
                    });
                    ctx.totalRowCount(3);
                }
        );
    }

    @Test
    public void testOnCaseDelete() {
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .deleteCommand(Department.class, 1L, DeleteMode.PHYSICAL)
                        .setDissociateAction(EmployeeProps.DEPARTMENT, DissociateAction.LAX),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from DEPARTMENT where ID = ?");
                    });
                    ctx.totalRowCount(1);
                }
        );
    }
}
