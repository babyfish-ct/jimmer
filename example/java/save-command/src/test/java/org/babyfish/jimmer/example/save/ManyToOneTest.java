package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.Book;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.example.save.model.BookProps;
import org.babyfish.jimmer.example.save.model.BookStore;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.SaveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Recommended learning sequence: 3
 *
 * <p>SaveModeTest -> IncompleteObjectTest -> [current: ManyToOneTest] ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest</p>
 */
public class ManyToOneTest extends AbstractMutationTest {

    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */

    @Test
    public void testShortAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                            book.setStore(
                                    ImmutableObjects.makeIdOnly(BookStore.class, 1L)
                            );
                        })
                );

        assertExecutedStatements(

                // Select data by id
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Data exists, update it.
                // The foreign key `store_id` is updated.
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testIllegalShortAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        SaveException ex = Assertions.assertThrows(SaveException.class, () -> {
            sql()
                    .getEntities()
                    .saveCommand(
                            BookDraft.$.produce(book -> {
                                book.setName("SQL in Action");
                                book.setEdition(1);
                                book.setPrice(new BigDecimal(49));
                                book.setStore(
                                        ImmutableObjects.makeIdOnly(BookStore.class, 99999L)
                                );
                            })
                    )
                    /*
                     * You can also use `setAutoIdOnlyTargetCheckingAll()`.
                     *
                     * If you use jimmer-spring-starter, it is unnecessary to
                     * do it because this switch is turned on.
                     *
                     * If the underlying `BOOK.STORE_ID` has foreign key constraints,
                     * even if this configuration is not used, error still will be
                     * raised by database so that you can choose not to use this
                     * configuration when you have strict performance requirements.
                     * However, this configuration can bring better error message.
                     *
                     * Sometimes it is not possible to add foreign key constraints,
                     * such table sharding. At this time, this configuration is
                     * very important.
                     */
                    .setAutoIdOnlyTargetChecking(BookProps.STORE)
                    .execute();
        });
        Assertions.assertEquals(
                "Save error caused by the path: " +
                        "\"<root>.store\": Illegal ids: [99999]",
                ex.getMessage()
        );

        assertExecutedStatements(

                // Is target id valid?
                new ExecutedStatement(
                        "select tb_1_.ID from BOOK_STORE tb_1_ where tb_1_.ID in (?)",
                        99999L
                )
        );
    }

    @Test
    public void testAssociationByKey() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                            book.applyStore(store -> {
                                store.setName("MANNING");
                            });
                        })
                );

        assertExecutedStatements(

                // Select parent object by key.
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // select aggregation-root object by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Aggregation-root object exists, update it, include the foreign key
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testAssociationByExistingParent() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        SimpleSaveResult<Book> result = sql().getEntities().save(
                BookDraft.$.produce(book -> {
                    book.setName("SQL in Action");
                    book.setEdition(1);
                    book.setPrice(new BigDecimal(49));
                    book.applyStore(store -> {
                        store.setName("MANNING");
                        store.setWebsite("https://www.manning.com");
                    });
                })
        );

        assertExecutedStatements(

                // Select parent by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // Parent exists, update it
                new ExecutedStatement(
                        "update BOOK_STORE set WEBSITE = ? where ID = ?",
                        "https://www.manning.com",
                        1L
                ),

                // Select aggregate-root object by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Aggregate-root object exists, update it
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(1, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(1, result.getAffectedRowCount(Book.class));
    }

    // This exception will never be raised if you use spring-data-jimmer
    // because this switch has already been turned on by it.
    @Test
    public void testAttachParentFailed() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        SaveException ex = Assertions.assertThrows(SaveException.class, () -> {
            sql().getEntities().save(
                    BookDraft.$.produce(book -> {
                        book.setName("SQL in Action");
                        book.setEdition(1);
                        book.setPrice(new BigDecimal(49));
                        book.applyStore(store -> {
                            store.setName("TURING");
                            store.setWebsite("https://www.turing.com");
                        });
                    })
            );
        });
        Assertions.assertEquals(
                "Save error caused by the path: \"<root>.store\": " +
                        "Cannot insert object because insert operation for this path is disabled, " +
                        "please call `setAutoAttaching(BookProps.STORE)` " +
                        "or `setAutoAttachingAll()` of the save command",
                ex.getMessage()
        );

        assertExecutedStatements(

                // Select the parent object by key.
                //
                // If no data selected, report error because the switch to
                // automatically create associated objects has not been turned on
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "TURING"
                )
        );
    }

    @Test
    public void testAttachParent() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                            book.applyStore(store -> {
                                store.setName("TURING");
                                store.setWebsite("https://www.turing.com");
                            });
                        })
                )
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unecessary to
                 * do it because this switch is turned on.
                 */
                .setAutoAttaching(BookProps.STORE)
                .execute();

        assertExecutedStatements(

                // Select parent by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "TURING"
                ),

                // Parent does not exist, however, the switch to automatically create
                // associated objects has not been turned on, so insert parent object.
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME, WEBSITE) values(?, ?)",
                        "TURING", "https://www.turing.com"
                ),

                // Select the aggregate-root by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Aggregate-root exists, update it, include the foreign key
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                )
        );

        Assertions.assertEquals(2, result.getTotalAffectedRowCount());
        Assertions.assertEquals(1, result.getAffectedRowCount(BookStore.class));
        Assertions.assertEquals(1, result.getAffectedRowCount(Book.class));
    }
}
