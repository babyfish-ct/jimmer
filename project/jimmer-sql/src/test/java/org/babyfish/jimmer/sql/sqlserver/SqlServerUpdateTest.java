package org.babyfish.jimmer.sql.sqlserver;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.babyfish.jimmer.sql.common.Constants.*;

/**
 * SQL Server UPDATE tests.
 *
 * SQL Server dialect properties:
 * - isUpdateAliasRequired() = true → "update alias SET ... FROM table alias"
 */
public class SqlServerUpdateTest extends AbstractMutationTest {

    @BeforeAll
    public static void beforeAll() {
        NativeDatabases.assumeNativeDatabase();
    }

    // ==================== Fluent Update Tests ====================

    /**
     * Test basic fluent update - update a single field on Author.
     * Verifies: SQL Server isUpdateAliasRequired=true → "update tb_1_ set ... from TABLE tb_1_ ..."
     */
    @Test
    public void fluentUpdateSingleField() {
        AuthorTable author = AuthorTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(author)
                        .set(author.lastName(), "Sammer")
                        .where(author.id().eq(sammerId)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set LAST_NAME = ? from AUTHOR tb_1_ where tb_1_.ID = ?");
                        it.variables("Sammer", sammerId);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test fluent update with multiple fields.
     * Verifies: Multiple SET clauses in SQL Server update alias format.
     */
    @Test
    public void fluentUpdateMultipleFields() {
        AuthorTable author = AuthorTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(author)
                        .set(author.firstName(), "Alexander")
                        .set(author.lastName(), "Banks")
                        .where(author.id().eq(alexId)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set FIRST_NAME = ?, LAST_NAME = ? from AUTHOR tb_1_ where tb_1_.ID = ?");
                        it.variables("Alexander", "Banks", alexId);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test fluent update with expression (price + 1).
     * Verifies: SQL Server generates "update tb_1_ set PRICE = tb_1_.PRICE + ? from BOOK tb_1_"
     */
    @Test
    public void fluentUpdateWithExpression() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(book.name().eq("Learning GraphQL")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set PRICE = tb_1_.PRICE + ? from BOOK tb_1_ where tb_1_.NAME = ?");
                    });
                    ctx.rowCount(3);
                }
        );
    }

    /**
     * Test fluent update setting to null.
     * Verifies: SQL Server handles null assignment correctly with alias format.
     */
    @Test
    public void fluentUpdateSetNull() {
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(store)
                        .set(store.website(), Expression.nullValue(String.class))
                        .where(store.name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set WEBSITE = null from BOOK_STORE tb_1_ where tb_1_.NAME = ?");
                        it.variables("MANNING");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test fluent update with complex AND condition.
     * Verifies: SQL Server generates compound WHERE clause with alias format.
     */
    @Test
    public void fluentUpdateWithComplexCondition() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.edition(), 99)
                        .where(book.name().eq("Learning GraphQL"))
                        .where(book.edition().eq(1)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set EDITION = ? from BOOK tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?");
                        it.variables(99, "Learning GraphQL", 1);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test fluent update with OR condition.
     * Verifies: SQL Server handles OR predicates in UPDATE WHERE clause.
     */
    @Test
    public void fluentUpdateWithOrCondition() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.price(), new BigDecimal("99.99"))
                        .where(Predicate.or(
                                book.name().eq("Learning GraphQL"),
                                book.name().eq("Effective TypeScript")
                        )),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set PRICE = ? from BOOK tb_1_ where tb_1_.NAME = ? or tb_1_.NAME = ?");
                        it.variables(new BigDecimal("99.99"), "Learning GraphQL", "Effective TypeScript");
                    });
                    ctx.rowCount(6);
                }
        );
    }

    /**
     * Test fluent update matching no rows (edge case).
     * Verifies: SQL Server executes UPDATE but affects 0 rows without error.
     */
    @Test
    public void fluentUpdateNoMatch() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.price(), new BigDecimal("0.01"))
                        .where(book.name().eq("NonExistentBook")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set PRICE = ? from BOOK tb_1_ where tb_1_.NAME = ?");
                        it.variables(new BigDecimal("0.01"), "NonExistentBook");
                    });
                    ctx.rowCount(0);
                }
        );
    }

    /**
     * Test fluent update with subquery in WHERE clause.
     * Verifies: SQL Server handles correlated subquery in UPDATE.
     */
    @Test
    public void fluentUpdateWithSubQuery() {
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(
                                book.id().in(
                                        getSqlClient(it2 -> it2.setDialect(new SqlServerDialect()))
                                                .createSubQuery(author)
                                                .where(author.firstName().eq("Alex"))
                                                .select(author.books().id())
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update tb_1_ set PRICE = tb_1_.PRICE + ? from BOOK tb_1_ " +
                                        "where tb_1_.ID in (" +
                                        "--->select tb_3_.BOOK_ID " +
                                        "--->from AUTHOR tb_2_ " +
                                        "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "--->where tb_2_.FIRST_NAME = ?" +
                                        ")"
                        );
                    });
                    ctx.rowCount(3);
                }
        );
    }

    // ==================== Multi-table JOIN Update Tests ====================

    /**
     * Test that SQL Server dialect forbids table joins in UPDATE statements.
     * Verifies: SQL Server throws ExecutionException when attempting to use
     * joined tables in an UPDATE statement (even for filtering purposes).
     */
    @Test
    public void fluentUpdateWithJoin() {
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .where(author.books().store().name().eq("MANNING")),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message("Table joins for update statement is forbidden by the current dialect, " +
                                "but there is a join 'Author.books.store'.");
                    });
                }
        );
    }

    // ==================== DissociateAction.SET_NULL via Update ====================

    /**
     * Test SET_NULL dissociation generates proper UPDATE SQL.
     * Verifies: When deleting a BookStore with SET_NULL, child BOOK.STORE_ID is updated to null first.
     * This uses the standard update (no alias needed for simple cases).
     */
    @Test
    public void updateSetNullForDisassociation() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        BookStore.class,
                        oreillyId
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.SET_NULL
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where STORE_ID = ?");
                        it.variables(oreillyId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID = ?");
                        it.variables(oreillyId);
                    });
                    ctx.totalRowCount(10); // 9 books (LGx3 + ETx3 + PTx3) SET NULL + 1 store DELETE
                }
        );
    }

    // ==================== Logical Delete via Update ====================

    /**
     * Test logical delete via UPDATE statement on Employee table.
     * Verifies: Logical delete generates "update EMPLOYEE tb_1_ set DELETED_MILLIS = ?" with alias.
     */
    @Test
    public void logicalDeleteViaUpdate() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(table)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update tb_1_ set DELETED_MILLIS = ? from EMPLOYEE tb_1_ " +
                                        "where " +
                                        "--->tb_1_.DEPARTMENT_ID = ? " +
                                        "and " +
                                        "--->tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }

    /**
     * Test physical delete via DELETE statement (no logical delete column).
     * Verifies: Physical delete still uses the correct DELETE alias format.
     */
    @Test
    public void physicalDeleteViaDelete() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(table)
                        .setMode(org.babyfish.jimmer.sql.ast.mutation.DeleteMode.PHYSICAL)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete tb_1_ from EMPLOYEE tb_1_ " +
                                        "where " +
                                        "--->tb_1_.DEPARTMENT_ID = ? " +
                                        "and " +
                                        "--->tb_1_.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }

    // ==================== Cascade Update / Edge Cases ====================

    /**
     * Test update with IN predicate for batch matching.
     * Verifies: SQL Server handles IN clause in UPDATE WHERE correctly.
     */
    @Test
    public void fluentUpdateWithInPredicate() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(book)
                        .set(book.price(), new BigDecimal("29.99"))
                        .where(book.id().in(Arrays.asList(
                                learningGraphQLId1,
                                learningGraphQLId2,
                                effectiveTypeScriptId1
                        ))),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set PRICE = ? from BOOK tb_1_ where tb_1_.ID in (?, ?, ?)");
                        it.variables(new BigDecimal("29.99"),
                                learningGraphQLId1, learningGraphQLId2, effectiveTypeScriptId1);
                    });
                    ctx.rowCount(3);
                }
        );
    }

    /**
     * Test update BookStore name directly.
     * Verifies: Simple entity update works with SQL Server alias format.
     */
    @Test
    public void updateBookStoreName() {
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createUpdate(store)
                        .set(store.name(), "O'REILLY UPDATED")
                        .where(store.id().eq(oreillyId)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set NAME = ? from BOOK_STORE tb_1_ where tb_1_.ID = ?");
                        it.variables("O'REILLY UPDATED", oreillyId);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test update then verify data consistency.
     * Performs an update and verifies the data was actually changed by querying back.
     */
    @Test
    public void updateThenVerifyData() {
        connectAndExpect(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                con -> {
                    // Execute update
                    int rowCount = getSqlClient(it -> it.setDialect(new SqlServerDialect()))
                            .createUpdate(AuthorTable.$)
                            .set(AuthorTable.$.lastName(), "UpdatedLastName")
                            .where(AuthorTable.$.id().eq(sammerId))
                            .execute(con);

                    // Verify by direct query
                    try (PreparedStatement stmt = con.prepareStatement(
                            "select LAST_NAME from AUTHOR where ID = ?"
                    )) {
                        stmt.setObject(1, sammerId);
                        ResultSet rs = stmt.executeQuery();
                        Assertions.assertTrue(rs.next(), "Author should exist");
                        String lastName = rs.getString("LAST_NAME");
                        Assertions.assertEquals("UpdatedLastName", lastName,
                                "Last name should be updated");
                    } catch (SQLException ex) {
                        Assertions.fail("Query failed: " + ex.getMessage());
                    }
                    return rowCount;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update tb_1_ set LAST_NAME = ? from AUTHOR tb_1_ where tb_1_.ID = ?");
                        it.variables("UpdatedLastName", sammerId);
                    });
                    ctx.value(result -> {
                        Assertions.assertEquals(1, result, "Should update exactly 1 row");
                    });
                }
        );
    }

    /**
     * Test CHECK dissociation fails when child references exist.
     * Verifies: SQL Server CHECK mode queries for existing references before allowing deletion.
     */
    @Test
    public void checkDisassociationFailsWithReferences() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).setDissociateAction(BookProps.STORE, DissociateAction.CHECK),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "where " +
                                        "--->tb_1_.STORE_ID = ? " +
                                        "--->offset ? rows fetch next ? rows only"
                        );
                        it.variables(manningId, 0L, 1);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                    });
                }
        );
    }
}
