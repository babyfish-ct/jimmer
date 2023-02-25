package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.example.save.model.BookProps;
import org.babyfish.jimmer.example.save.model.Gender;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class ManyToManyTest extends AbstractMutationTest {

    @Test
    public void insertMiddleTableByShortAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );
        jdbc(
                "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
                100L, "Ben", "Brumm", "M"
        );

        sql().getEntities().save(
                BookDraft.$.produce(book -> {
                    book.setName("SQL in Action");
                    book.setEdition(1);
                    book.setPrice(new BigDecimal(49));
                    book.addIntoAuthors(author -> author.setId(100L));
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
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                ),
                new ExecutedStatement(
                        "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                        10L
                ),
                new ExecutedStatement(
                        "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?)",
                        10L, 100L
                )
        );
    }

    @Test
    public void insertMiddleTableByIllegalShortAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        Assertions.assertThrows(ExecutionException.class, () -> {
            sql().getEntities().save(
                    BookDraft.$.produce(book -> {
                        book.setName("SQL in Action");
                        book.setEdition(1);
                        book.setPrice(new BigDecimal(49));
                        book.addIntoAuthors(author -> author.setId(99999L));
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
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                ),
                new ExecutedStatement(
                        "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                        10L
                ),
                new ExecutedStatement(
                        "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?)",
                        10L, 99999L
                )
        );
    }
    
    @Test
    public void deleteMiddleTable() {
        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );
        jdbc(
                "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
                100L, "Ben", "Brumm", "M"
        );
        jdbc(
                "insert into author(id, first_name, last_name, gender) values(?, ?, ?, ?)",
                200L, "Prabath", "Siriwardena", "M"
        );
        jdbc("insert into book_author_mapping(book_id, author_id) values(?, ?)", 10L, 100L);
        jdbc("insert into book_author_mapping(book_id, author_id) values(?, ?)", 10L, 200L);

        sql().getEntities().save(
                BookDraft.$.produce(book -> {
                    book.setName("SQL in Action");
                    book.setEdition(1);
                    book.setPrice(new BigDecimal(49));
                    book.addIntoAuthors(author -> author.setId(100L));
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
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                ),
                new ExecutedStatement(
                        "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                        10L
                ),
                new ExecutedStatement(
                        "delete from BOOK_AUTHOR_MAPPING " +
                                "where (BOOK_ID, AUTHOR_ID) in ((?, ?))",
                        10L, 200L
                )
        );
    }

    @Test
    public void testCannotInsertNewAuthorByLongAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        Assertions.assertThrows(ExecutionException.class, () -> {
            sql().getEntities().save(
                    BookDraft.$.produce(book -> {
                        book.setName("SQL in Action");
                        book.setEdition(1);
                        book.setPrice(new BigDecimal(49));
                        book.addIntoAuthors(author -> {
                            author.setFirstName("Ben");
                            author.setLastName("Brumm");
                            author.setGender(Gender.MALE);
                        });
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
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                "from AUTHOR as tb_1_ " +
                                "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?",
                        "Ben", "Brumm"
                )
        );
    }

    @Test
    public void testInsertNewAuthorByLongAssociation() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                            book.addIntoAuthors(author -> {
                                author.setFirstName("Ben");
                                author.setLastName("Brumm");
                                author.setGender(Gender.MALE);
                            });
                        })
                )
                .setAutoAttaching(BookProps.AUTHORS)
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                ),
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                "from AUTHOR as tb_1_ " +
                                "where tb_1_.FIRST_NAME = ? and tb_1_.LAST_NAME = ?",
                        "Ben", "Brumm"
                ),
                new ExecutedStatement(
                        "insert into AUTHOR(FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?)",
                        "Ben", "Brumm", "M"
                ),
                new ExecutedStatement(
                        "select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?",
                        10L
                ),
                new ExecutedStatement(
                        "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?)",
                        10L, 100L
                )
        );
    }
}
