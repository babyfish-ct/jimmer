package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.filter.common.FileFilter;
import org.babyfish.jimmer.sql.meta.LogicalDeletedLongGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.filter.File;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.hr.EmployeeProps;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

public class DeleteTest extends AbstractMutationTest {

    @Test
    public void testDeleteBookStoreOnDissociateLax() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).setDissociateAction(BookProps.STORE, DissociateAction.LAX),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_STORE " +
                                        "where ID = ?"
                        );
                        it.variables(manningId);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                    });
                }
        );
    }

    @Test
    public void testDeleteBookStoreOnDissociateChecking() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).setDissociateAction(BookProps.STORE, DissociateAction.CHECK),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.STORE_ID = ? " +
                                        "limit ?"
                        );
                        it.variables(manningId, 1);
                    });
                    ctx.throwable(it -> {
                        it.type(SaveException.CannotDissociateTarget.class);
                        it.message(
                                "Save error caused by the path: \"<root>.books\": " +
                                        "Cannot dissociate child objects because " +
                                        "the dissociation action of the many-to-one property " +
                                        "\"org.babyfish.jimmer.sql.model.Book.store\" " +
                                        "is not configured as \"set null\" or \"cascade\". " +
                                        "There are two ways to resolve this issue: " +
                                        "Decorate the many-to-one property \"org.babyfish.jimmer.sql.model.Book.store\" " +
                                        "by @org.babyfish.jimmer.sql.OnDissociate whose argument is " +
                                        "`DissociateAction.SET_NULL` or `DissociateAction.DELETE`, " +
                                        "or use save command's runtime configuration to override it"
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
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.SET_NULL
                ),
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
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * from BOOK tb_2_ " +
                                        "--->where tb_1_.BOOK_ID = tb_2_.ID " +
                                        "--->and tb_2_.STORE_ID = ?" +
                                        ")"
                        );
                        it.unorderedVariables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where STORE_ID = ?");
                        it.unorderedVariables(manningId);
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
    public void testDeleteBook() {
        UUID nonExistingId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().deleteAllCommand(
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
                        it.sql("delete from ADMINISTRATOR_METADATA where ADMINISTRATOR_ID = ?");
                        it.variables(1L);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from ADMINISTRATOR where ID = ?");
                    });
                    ctx.rowCount(AffectedTable.of(AdministratorProps.ROLES), 1);
                    ctx.rowCount(AffectedTable.of(AdministratorMetadata.class), 1);
                    ctx.rowCount(AffectedTable.of(Administrator.class), 1);
                }
        );
    }

    @Test
    public void deleteTreeByDepth0() {
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setMaxCommandJoinCount(0);
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID = ?"
                        );
                    });
                    ctx.totalRowCount(24);
                }
        );
    }

    @Test
    public void deleteTreeByDepth1() {
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setMaxCommandJoinCount(1);
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "where tb_2_.PARENT_ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "where tb_2_.PARENT_ID in (?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "where tb_2_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID in (?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in (?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID = ?");
                    });
                    ctx.totalRowCount(24);
                }
        );
    }

    @Test
    public void deleteTreeByDepth2() {
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setMaxCommandJoinCount(2);
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ?" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ?");
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
                        it.sql("update DEPARTMENT set DELETED_MILLIS = ? where ID = ?");
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
                                "select tb_1_.ID " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? " +
                                        "and tb_1_.DELETED_MILLIS = ? " +
                                        "limit ?"
                        );
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Save error caused by the path: \"<root>.employees\": " +
                                        "Cannot dissociate child objects because the dissociation action of the " +
                                        "many-to-one property \"org.babyfish.jimmer.sql.model.hr.Employee.department\" " +
                                        "is not configured as \"set null\" or \"cascade\". " +
                                        "There are two ways to resolve this issue: " +
                                        "Decorate the many-to-one property " +
                                        "\"org.babyfish.jimmer.sql.model.hr.Employee.department\" " +
                                        "by @org.babyfish.jimmer.sql.OnDissociate whose argument is " +
                                        "`DissociateAction.SET_NULL` or `DissociateAction.DELETE`, " +
                                        "or use save command's runtime configuration to override it"
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
                                "update EMPLOYEE " +
                                        "set DEPARTMENT_ID = null " +
                                        "where DEPARTMENT_ID = ? and DELETED_MILLIS = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("update DEPARTMENT set DELETED_MILLIS = ? where ID = ?");
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
                        it.sql("delete from EMPLOYEE where DEPARTMENT_ID = ?");
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
                LogicalDeletedLongGenerator.class,
                ImmutableType.get(Employee.class).getLogicalDeletedInfo().getGeneratorType()
        );
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Department.class,
                        1L
                ).setDissociateAction(
                        EmployeeProps.DEPARTMENT,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID = ? " +
                                        "and DELETED_MILLIS = ?"
                        );
                        it.variables(UNKNOWN_VARIABLE, 1L, 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("update DEPARTMENT set DELETED_MILLIS = ? where ID = ?");
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

    @Test
    public void testDeleteBookWithoutAuthorsForIssue644() {
        UUID id = UUID.fromString("c0d28339-f14b-43d0-a193-6f98d39f1cd8");
        connectAndExpect(
                con -> {
                    try (PreparedStatement stmt = con.prepareStatement(
                            "insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)")
                    ) {
                        stmt.setObject(1, id);
                        stmt.setString(2, "Jimmer in Action");
                        stmt.setInt(3, 1);
                        stmt.setBigDecimal(4, new BigDecimal("69.9"));
                        stmt.executeUpdate();
                    } catch (SQLException ex) {
                        Assertions.fail("Failed to insert lonely book");
                    }
                    return getSqlClient(it -> it.setDialect(new H2Dialect()))
                            .getEntities()
                            .deleteCommand(Book.class, id)
                            .execute(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ?");
                        it.variables(id);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID = ?");
                        it.variables(id);
                    });
                    ctx.value(result -> {
                        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
                    });
                }
        );
    }
}
