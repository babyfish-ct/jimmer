package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.example.save.model.BookProps;
import org.babyfish.jimmer.example.save.model.BookStore;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class ManyToOneTest extends AbstractMutationTest {

    @Test
    public void testShortAssociationById() {

        jdbc("insert into book_store(id, name) values(?, ?)", 100L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                            book.setStore(
                                    ImmutableObjects.makeIdOnly(BookStore.class, 100L)
                            );
                        })
                );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 100L, 200L
                )
        );
    }

    @Test
    public void testShortAssociationByKey() {

        jdbc("insert into book_store(id, name) values(?, ?)", 100L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        sql()
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
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
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
                        new BigDecimal(49), 100L, 200L
                )
        );
    }

    @Test
    public void testIllegalShortAssociationById() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        Assertions.assertThrows(ExecutionException.class, () -> {
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

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 99999L, 200L
                )
        );
    }

    @Test
    public void testIllegalShortAssociationByKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        Assertions.assertThrows(ExecutionException.class, () -> {
            sql()
                    .getEntities()
                    .save(
                            BookDraft.$.produce(book -> {
                                book.setName("SQL in Action");
                                book.setEdition(1);
                                book.setPrice(new BigDecimal(49));
                                book.applyStore(store -> {
                                    store.setName("IllegalName");
                                });
                            })
                    );
        });

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME from BOOK_STORE as tb_1_ where tb_1_.NAME = ?",
                        "IllegalName"
                )
        );
    }

    @Test
    public void testLongAssociationByExistingParent() {

        jdbc("insert into book_store(id, name) values(?, ?)", 100L, "MANNING");
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        sql().getEntities().save(
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
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "MANNING"
                ),
                new ExecutedStatement(
                        "update BOOK_STORE set WEBSITE = ? where ID = ?",
                        "https://www.manning.com",
                        100L
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 100L, 200L
                )
        );
    }

    @Test
    public void testLongAssociationByNonExistingParentAndNotAllowedToCreate() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                200L, "SQL in Action", 1, new BigDecimal(45));

        Assertions.assertThrows(ExecutionException.class, () -> {
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

        assertExecutedStatements(
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
                200L, "SQL in Action", 1, new BigDecimal(45));

        sql()
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
                .configure(cfg -> cfg.setAutoAttaching(BookProps.STORE))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME " +
                                "from BOOK_STORE as tb_1_ " +
                                "where tb_1_.NAME = ?",
                        "TURING"
                ),
                new ExecutedStatement(
                        "insert into BOOK_STORE(NAME, WEBSITE) values(?, ?)",
                        "TURING", "https://www.turing.com"
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ?, STORE_ID = ? where ID = ?",
                        new BigDecimal(49), 1L, 200L
                )
        );
    }
}
