package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.example.save.model.BookProps;
import org.babyfish.jimmer.example.save.model.BookStoreDraft;
import org.babyfish.jimmer.example.save.model.BookStoreProps;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class OneToManyTest extends AbstractMutationTest {

    @Test
    public void testAttachChildByShortAssociationBasedOnId() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45)
        );

        sql().getEntities().save(
                BookStoreDraft.$.produce(store -> {
                    store.setName("MANNING");
                    store.addIntoBooks(book -> book.setId(10L));
                })
        );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME) values(?)",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                )
        );
    }

    @Test
    public void testAttachChildByShortAssociationBasedOnKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45)
        );

        sql().getEntities().save(
                BookStoreDraft.$.produce(store -> {
                    store.setName("MANNING");
                    store.addIntoBooks(book -> book.setName("SQL in Action").setEdition(1));
                })
        );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME) values(?)",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID = ?",
                        1L, 10L
                )
        );
    }

    @Test
    public void testUpdateChildByLongAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );

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

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 10L
                ),
                new ExecutedStatement(
                        "select 1 from BOOK " +
                                "where STORE_ID = ? and ID not in(?) " +
                                "limit ?",
                        1L, 10L, 1
                )
        );
    }

    @Test
    public void testCannotInsertChildByLongAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");

        Assertions.assertThrows(ExecutionException.class, () -> {
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

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
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
    public void testInsertChildByLongAssociation() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");

        sql()
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
                .setAutoAttaching(BookStoreProps.BOOKS)
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "select " +
                                "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?)",
                        "SQL in Action", 1, new BigDecimal(49), 1L
                ),
                new ExecutedStatement(
                        "select 1 from BOOK where STORE_ID = ? and ID not in(?) limit ?",
                        1L, 10L, 1
                )
        );
    }
    
    @Test
    public void testDetachChildNotAllowed() {

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

        Throwable ex = Assertions.assertThrows(ExecutionException.class, () -> {
            sql().getEntities().save(
                    BookStoreDraft.$.produce(store -> {
                        store.setName("MANNING");
                        store.addIntoBooks(book -> book.setId(10L));
                    })
            );
        });

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),
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
                10,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                20,
                "GraphQL in Action",
                1,
                new BigDecimal(39),
                1L
        );

        sql()
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
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = null " +
                                "where STORE_ID = ? and ID not in (?)",
                        1L, 10L
                )
        );
    }

    @Test
    public void testDetachChildByDeletingChild() {

        jdbc("insert into book_store(id, name) values(?, ?)", 1L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                10,
                "SQL in Action",
                1,
                new BigDecimal(45),
                1L
        );
        jdbc(
                "insert into book(id, name, edition, price, store_id) values(?, ?, ?, ?, ?)",
                20,
                "GraphQL in Action",
                1,
                new BigDecimal(39),
                1L
        );

        sql()
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
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "update BOOK set STORE_ID = ? where ID in (?)",
                        1L, 10L
                ),
                new ExecutedStatement(
                        "select ID from BOOK where STORE_ID = ? and ID not in (?)",
                        1L, 10L
                ),
                new ExecutedStatement(
                        "delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?)",
                        20L
                ),
                new ExecutedStatement(
                        "delete from BOOK where ID in (?)",
                        20L
                )
        );
    }
}
