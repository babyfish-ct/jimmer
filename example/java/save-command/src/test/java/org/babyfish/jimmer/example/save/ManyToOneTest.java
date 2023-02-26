package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.Book;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.example.save.model.BookProps;
import org.babyfish.jimmer.example.save.model.BookStore;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
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
                                "from BOOK as tb_1_ " +
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
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),

                // select aggregation-root object by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
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
    public void testIllegalShortAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45));

        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, () -> {
                sql()
                        .getEntities()
                        .save(
                                BookDraft.$.produce(book -> {
                                    book.setName("SQL in Action");
                                    book.setEdition(1);
                                    book.setPrice(new BigDecimal(49));
                                    book.setStore(
                                            ImmutableObjects.makeIdOnly(BookStore.class, 99999L)
                                    );
                                })
                        );
        });
        Assertions.assertEquals(
                "Cannot execute SQL statement: " +
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?, " +
                        "variables: [49, 99999, 10]",
                ex.getMessage()
        );
        /*
         * In the current Jimmer, the many-to-one property is based on the foreign key.
         * If the associated object holds an illegal id, the database will report an error.
         *
         * In the future, Jimmer will support fake foreign key(It should be understood
         * as a foreign key in business, but it is not a foreign key in the database.
         * It is suitable for the database sharding and table sharding), an additional
         * validation will be added here.
         */

        assertExecutedStatements(

                // Select aggregate-root object by
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Aggregate-root exists, update it with illegal foreign key
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 99999L, 10L
                )
        );
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
                                "from BOOK_STORE as tb_1_ " +
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
                                "from BOOK as tb_1_ " +
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

    @Test
    public void testAssociationByNonExistingParentAndNotAllowedToCreate() {

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
                "Save error caused by the path: \"<root>.store\": Cannot insert object because insert operation for this path is disabled",
                ex.getMessage()
        );

        assertExecutedStatements(

                // Select the parent object by key.
                //
                // If no data selected, report error because the switch to
                // automatically create associated objects has not been turned on
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "TURING"
                )
        );
    }

    @Test
    public void testLongAssociationByNonExistingParentAndAllowToCreate() {

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
                                "from BOOK_STORE as tb_1_ " +
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
                                "from BOOK as tb_1_ " +
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
