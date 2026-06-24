package org.babyfish.jimmer.sql.sqlserver;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.Author;
import org.babyfish.jimmer.sql.model.AuthorProps;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.*;

/**
 * SQL Server DELETE tests.
 *
 * SQL Server dialect properties:
 * - isDeletedAliasRequired() = true  → "delete alias from table alias"
 * - isTableOfSubQueryMutable() = true → direct EXISTS subquery (no SELECT-ID degradation)
 * - isAnyEqualityOfArraySupported() = false → uses tuple/IN predicates (not array ANY)
 * - isTupleSupported() = false → falls back to non-tuple comparison
 */
public class SqlServerDeleteTest extends AbstractMutationTest {
    @BeforeAll
    public static void beforeAll() {
        NativeDatabases.assumeNativeDatabase();
    }

    /**
     * Test simple delete - delete a Book with middle table associations.
     * Verifies: basic delete from middle table + target table (no alias needed for simple cases).
     */
    @Test
    public void deleteBook() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        Book.class,
                        learningGraphQLId1
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID = ?");
                    });
                    ctx.totalRowCount(3);
                }
        );
    }

    /**
     * Test fluent delete - delete Books by condition (disable dissociation).
     * Verifies: SQL Server isDeletedAliasRequired=true → "delete tb_1_ from BOOK tb_1_"
     */
    @Test
    public void fluentDelete() {
        BookTable bookTable = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(bookTable)
                .where(bookTable.name().eq("Learning GraphQL"))
                .disableDissociation(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete tb_1_ from BOOK tb_1_ where tb_1_.NAME = ?");
                        it.variables("Learning GraphQL");
                    });
                    ctx.rowCount(3);
        }
        );
    }

    /**
     * Test cascade delete with depth=0 (ChildTableOperator top level).
     * Delete "Drinks"(id=3) whose children (CocaCola, Fanta) are leaf nodes.
     *
     * SQL Server: isTableOfSubQueryMutable=true → uses direct EXISTS subquery
     * for depth=1 (delete children of child nodes). When the subquery is empty
     * (leaf nodes have no children), 0 rows are affected.
     * depth=0 deletes use plain "delete from TABLE where ..." (no alias needed).
     *
     * Note: TOO_DEEP query is triggered when depth exceeds maxCommandJoinCount (default=2)
     */
    @Test
    public void deleteTreeNodeDepth0() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        3L   // Drinks node, children: CocaCola(4), Fanta(5)
                ),
                ctx -> {
                    // TOO_DEEP: Query nodes with depth > maxCommandJoinCount
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ?"
                        );
                        it.variables(3L);
                    });
                    // depth=1: delete children of child nodes (empty for leaf nodes)
                    ctx.statement(it -> {
                        it.sql(
                                "delete tb_1_ from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ?" +
                                        ")"
                        );
                        it.variables(3L);
                    });
                    // depth=0: delete child nodes (CocaCola, Fanta)
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ?");
                        it.variables(3L);
                    });
                    // delete Drinks itself (top-level target)
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID = ?");
                        it.variables(3L);
                    });
                    ctx.totalRowCount(3); // CocaCola + Fanta + Drinks
                }
        );
    }

    /**
     * Test cascade delete with depth>0 (nested ChildTableOperator).
     * Delete "Food"(id=2) whose children (Drinks, Bread) have their own children.
     *
     * SQL Server: isTableOfSubQueryMutable=true → uses direct EXISTS subquery
     * (unlike MySQL's SELECT-ID degradation). The depth=1 delete uses
     * "delete tb_1_ from TABLE tb_1_ where exists(...)" with alias.
     * depth=0 deletes use plain "delete from TABLE where ..." (no alias needed).
     *
     * Tree structure:
     *   Food(2)
     *   ├── Drinks(3) → CocaCola(4), Fanta(5)
     *   └── Bread(6)  → Baguette(7), Ciabatta(8)
     *
     * Note: TOO_DEEP query is triggered when depth exceeds maxCommandJoinCount (default=2)
     */
    @Test
    public void deleteTreeNodeDepthGreaterThan0() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        2L   // Food node
                ),
                ctx -> {
                    // TOO_DEEP: Query nodes with depth > maxCommandJoinCount
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ?"
                        );
                        it.variables(2L);
                    });
                    // depth=1: delete children of child nodes (CocaCola, Fanta, Baguette, Ciabatta)
                    ctx.statement(it -> {
                        it.sql(
                                "delete tb_1_ from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ?" +
                                        ")"
                        );
                        it.variables(2L);
                    });
                    // depth=0: delete child nodes (Drinks, Bread)
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ?");
                        it.variables(2L);
                    });
                    // delete Food itself (top-level target)
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID = ?");
                        it.variables(2L);
                    });
                    ctx.totalRowCount(7); // 4 leaves + 2 mid-level + 1 root
                }
        );
    }

    /**
     * Test batch delete (deleteAllCommand) - delete multiple Books by IDs.
     * Verifies: IN-based batch deletion with middle table cleanup.
     * Includes a non-existing ID to verify it is safely ignored.
     * SQL Server: queries middle table rows first (SELECT+IN), then deletes them.
     */
    @Test
    public void deleteBooksBatch() {
        UUID nonExistingId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteAllCommand(
                        Book.class,
                        Arrays.asList(
                                learningGraphQLId1,
                                learningGraphQLId2,
                                nonExistingId
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in (?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 4);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                }
        );
    }

    /**
     * Test delete Author with multiple middle table associations.
     * Verifies: AUTHOR_COUNTRY_MAPPING and BOOK_AUTHOR_MAPPING are cleaned up
     * before the AUTHOR record itself is deleted.
     */
    @Test
    public void deleteAuthor() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        Author.class,
                        alexId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select COUNTRY_CODE from AUTHOR_COUNTRY_MAPPING where AUTHOR_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR_COUNTRY_MAPPING where AUTHOR_ID = ? and COUNTRY_CODE = ?");
                        it.variables(alexId, "USA");
                    });
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ? and BOOK_ID = ?");
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

    /**
     * Test delete BookStore with DissociateAction.LAX (ignore child constraint violations).
     * SQL Server: simple delete without checking child references.
     */
    @Test
    public void deleteBookStoreWithLax() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).setDissociateAction(BookProps.STORE, DissociateAction.LAX),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID = ?");
                        it.variables(manningId);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                    });
                }
        );
    }

    /**
     * Test delete BookStore with DissociateAction.CHECK (verify no child references exist).
     * Verifies: SQL Server generates SELECT check query with pagination syntax.
     * On SQL Server, FK constraint may fire before application-level check,
     * resulting in ExecutionException.
     */
    @Test
    public void deleteBookStoreWithCheck() {
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

    /**
     * Test delete BookStore with DissociateAction.SET_NULL (nullify child foreign keys).
     * Verifies: SQL Server updates child BOOK.STORE_ID to null before deleting the store.
     */
    @Test
    public void deleteBookStoreWithSetNull() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
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
                    ctx.totalRowCount(4)
                            .rowCount(AffectedTable.of(Book.class), 3)
                            .rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    /**
     * Test delete BookStore with DissociateAction.DELETE (cascade delete children).
     * Verifies: SQL Server queries middle table rows first (SELECT+JOIN),
     * then deletes them individually, then deletes child books, then the store.
     *
     * SQL Server specific:
     * - BOOK_AUTHOR_MAPPING: "select ... inner join ..." to find rows, then batch delete
     * - BOOK: direct "delete from BOOK where STORE_ID = ?"
     * - BOOK_STORE: direct delete by ID
     */
    @Test
    public void deleteBookStoreWithCascadeDelete() {
        executeAndExpectResult(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.BOOK_ID, tb_1_.AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "inner join BOOK tb_2_ on tb_1_.BOOK_ID = tb_2_.ID " +
                                        "where " +
                                        "--->tb_2_.STORE_ID = ?"
                        );
                        it.unorderedVariables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID = ?");
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

    /**
     * Test fluent delete with complex conditions (multiple predicates).
     * Verifies: SQL Server generates "delete alias from TABLE alias" with compound WHERE clause.
     */
    @Test
    public void fluentDeleteWithComplexCondition() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(book)
                .where(book.name().eq("Learning GraphQL"))
                .where(book.edition().eq(1))
                .disableDissociation(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete tb_1_ from BOOK tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?");
                        it.variables("Learning GraphQL", 1);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    /**
     * Test fluent delete with OR condition.
     * Verifies: SQL Server correctly handles OR predicates in DELETE WHERE clause.
     */
    @Test
    public void fluentDeleteWithOrCondition() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(book)
                .where(Predicate.or(book.name().eq("Learning GraphQL"), (book.name().eq("Effective TypeScript"))))
                .disableDissociation(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete tb_1_ from BOOK tb_1_ where tb_1_.NAME = ? or tb_1_.NAME = ?");
                        it.variables("Learning GraphQL", "Effective TypeScript");
                    });
                    ctx.rowCount(6);
                }
        );
    }

    /**
     * Test fluent delete matching no rows (edge case).
     * Verifies: SQL Server executes DELETE but affects 0 rows without error.
     */
    @Test
    public void fluentDeleteNoMatch() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SqlServerDialect());
                }).createDelete(book)
                .where(book.name().eq("NonExistentBook"))
                .disableDissociation(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete tb_1_ from BOOK tb_1_ where tb_1_.NAME = ?");
                        it.variables("NonExistentBook");
                    });
                    ctx.rowCount(0);
                }
        );
    }

    /**
     * Test delete book that has no author associations (edge case).
     * Verifies: Middle table cleanup SQL is still generated even when no mappings exist,
     * and the book itself is deleted successfully.
     * Based on issue #644 scenario adapted for SQL Server.
     */
    @Test
    public void deleteBookWithoutAuthors() {
        UUID id = UUID.fromString("c0d28339-f14b-43d0-a193-6f98d39f1cd8");
        connectAndExpect(
                NativeDatabases.SQLSERVER_DATA_SOURCE,
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
                    return getSqlClient(it -> it.setDialect(new SqlServerDialect()))
                            .getEntities()
                            .deleteCommand(Book.class, id)
                            .execute(con);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?");
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
