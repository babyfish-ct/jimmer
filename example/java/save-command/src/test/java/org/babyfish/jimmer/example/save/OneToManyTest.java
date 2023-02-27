package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.*;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Recommended learning sequence: 4
 *
 * <p>SaveModeTest -> IncompleteObjectTest -> ManyToOneTest ->
 * [current: OneToManyTest] -> ManyToManyTest -> RecursiveTest -> TriggerTest</p>
 */
public class OneToManyTest extends AbstractMutationTest {

    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */

    @Test
    public void testAttachChildByShortAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L,
                "SQL in Action",
                1,
                new BigDecimal(45)
        );

        SimpleSaveResult<BookStore> result = sql().getEntities().save(
                BookStoreDraft.$.produce(store -> {
                    store.setName("MANNING");
                    store.addIntoBooks(book -> book.setId(10L));
                })
        );

        assertExecutedStatements(

                // Select aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Aggregate does not exist, insert it
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME) values(?)",
                        "MANNING"
                ),

                // Change the foreign key of child object
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(1, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(1, result.getAffectedRowCount(Book.class));
    }

    @Test
    public void testAttachChildByAssociationBasedOnKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45)
        );

        SimpleSaveResult<BookStore> result = sql().getEntities().save(
                BookStoreDraft.$.produce(store -> {
                    store.setName("MANNING");
                    store.addIntoBooks(book -> book.setName("SQL in Action").setEdition(1));
                })
        );

        assertExecutedStatements(

                // Select aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Aggregate does not exist, insert it
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME) values(?)",
                        "MANNING"
                ),

                // Select child object by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Child object exists, update it, include foreign key
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID = ?",
                        1L, 10L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(1, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(1, result.getAffectedRowCount(Book.class));
    }

    @Test
    public void testUpdateWithAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );

        SimpleSaveResult<BookStore> result = sql().getEntities().save(
                BookStoreDraft.$.produce(store -> {
                    store.setName("MANNING");
                    store.addIntoBooks(book -> {
                        book.setName("SQL in Action");
                        book.setEdition(1);
                        book.setPrice(new BigDecimal(49));
                    });
                })
        );

        assertExecutedStatements(

                // Select aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Select child object by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Child object exists, update it, include foreign key
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                ),

                // The aggregate-root exists, so there may be more child objects in the database,
                // query whether there are other child objects that need to be dissociated
                // besides the saved child objects
                //
                // In this test case, no more child objects will be found
                new ExecutedStatement(
                        "select 1 from BOOK " +
                                "where STORE_ID = ? and ID not in(?) " +
                                "limit ?",
                        1L, 10L, 1
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testAttachChildFailed() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");

        SaveException ex = Assertions.assertThrows(SaveException.class, () -> {
                    sql().getEntities().save(
                            BookStoreDraft.$.produce(store -> {
                                store.setName("MANNING");
                                store.addIntoBooks(book -> {
                                    book.setName("SQL in Action");
                                    book.setEdition(1);
                                    book.setPrice(new BigDecimal(49));
                                });
                            })
                    );
        });
        Assertions.assertEquals(
                "Save error caused by the path: \"<root>.books\": " +
                        "Cannot insert object because insert operation for this path is disabled, " +
                        "please call `setAutoAttaching(BookStoreProps.BOOKS)` or " +
                        "`setAutoAttachingAll()` of the save command",
                ex.getMessage()
        );

        assertExecutedStatements(

                // Query aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Query child object by key
                // In this test case, nothing will be found, it need to be inserted.
                // However, the switch to automatically create associated objects
                // has not been turned on so that error will be raised
                new ExecutedStatement(
                        "select " +
                                "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                )
        );
    }

    @Test
    public void testAttachChild() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");

        SimpleSaveResult<BookStore> result = sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("MANNING");
                            store.addIntoBooks(book -> {
                                book.setName("SQL in Action");
                                book.setEdition(1);
                                book.setPrice(new BigDecimal(49));
                            });
                        })
                )
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unecessary to
                 * do it because this switch is turned on.
                 */
                .setAutoAttaching(BookStoreProps.BOOKS)
                .execute();

        assertExecutedStatements(

                // Select aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Select parent object by key
                new ExecutedStatement(
                        "select " +
                                "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Child object does not exist, insert it
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?)",
                        "SQL in Action", 1, new BigDecimal(49), 1L
                ),

                // The aggregate-root exists, so there may be more child objects in the database,
                // query whether there are other child objects that need to be dissociated
                // besides the saved child objects
                //
                // In this test case, no more child objects will be found
                new ExecutedStatement(
                        "select 1 from BOOK where STORE_ID = ? and ID not in(?) limit ?",
                        1L, 10L, 1
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }
    
    @Test
    public void testDetachChildFailed() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45)
        );
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                20,
                "GraphQL in Action",
                1,
                new BigDecimal(39),
                1L
        );

        SaveException ex = Assertions.assertThrows(SaveException.class, () -> {
            sql().getEntities().save(
                    BookStoreDraft.$.produce(store -> {
                        store.setName("MANNING");
                        store.addIntoBooks(book -> book.setId(10L));
                    })
            );
        });
        Assertions.assertEquals(
                "Save error caused by the path: \"<root>.books\": " +
                        "Cannot dissociate child objects because the dissociation action of the many-to-one property " +
                        "\"org.babyfish.jimmer.example.save.model.Book.store\" is not configured as \"set null\" or \"cascade\". " +
                        "There are two ways to resolve this issue: " +
                        "Decorate the many-to-one property \"org.babyfish.jimmer.example.save.model.Book.store\" " +
                        "by @org.babyfish.jimmer.sql.OnDissociate whose argument is " +
                        "`DissociateAction.SET_NULL` or `DissociateAction.DELETE` , " +
                        "or use save command's runtime configuration to override it",
                ex.getMessage()
        );

        assertExecutedStatements(

                // Query aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Update the foreign key of child object(s)
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),

                // The aggregate-root exists, so there may be more child objects in the database,
                // query whether there are other child objects that need to be dissociated
                // besides the saved child objects
                //
                // In this test case, child objects will be found but jimmer doesn't know
                // how to dissociate them so that error is raised
                new ExecutedStatement(
                        "select 1 from BOOK " +
                                "where STORE_ID = ? and ID not in(?) " +
                                "limit ?",
                        1L, 10L, 1
                )
        );
    }

    @Test
    public void testDetachChildByClearingForeignKey() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                10L,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                20L,
                "GraphQL in Action",
                1,
                new BigDecimal(39),
                1L
        );

        SimpleSaveResult<BookStore> result = sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("MANNING");
                            store.addIntoBooks(book -> book.setId(10L));
                        })
                )
                .setDissociateAction(BookProps.STORE, DissociateAction.SET_NULL)
                .execute();

        assertExecutedStatements(

                // Query aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Update foreign key of child object(s)
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),

                // The aggregate-root exists, so there may be more child objects in the database,
                // clear the foreign key of them.
                new ExecutedStatement(
                        "update BOOK set STORE_ID = null " +
                                "where STORE_ID = ? and ID not in (?)",
                        1L, 10L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(0, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(2, result.getAffectedRowCount(Book.class));
    }

    @Test
    public void testDetachChildByDeletingChild() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                10L,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                20L,
                "GraphQL in Action",
                1,
                new BigDecimal(39),
                1L
        );

        SimpleSaveResult<BookStore> result = sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("MANNING");
                            store.addIntoBooks(book -> book.setId(10L));
                        })
                )
                .setDissociateAction(BookProps.STORE, DissociateAction.DELETE)
                .execute();

        assertExecutedStatements(

                // Query aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Update foreign key of child objects.
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),

                // The aggregate-root exists, so there may be more child objects in the database,
                // select id of them.
                new ExecutedStatement(
                        "select ID from BOOK where STORE_ID = ? and ID not in (?)",
                        1L, 10L
                ),

                // Jimmer found `book-20` must be deleted,
                // Before doing this, the reference to `books-20` in the middle table of
                // the many-to-many association `Book.authors` must be removed.
                new ExecutedStatement(
                        "delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?)",
                        20L
                ),

                // Now, `book-20` can be deleted safely
                new ExecutedStatement(
                        "delete from BOOK where ID in (?)",
                        20L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(0, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(2, result.getAffectedRowCount(Book.class));
    }
}
