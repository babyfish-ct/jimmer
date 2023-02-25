package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class SaveModeTest extends AbstractMutationTest {

    @Test
    public void testInsertOnly() {

        sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.INSERT_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                        "SQL in Action",
                        1,
                        new BigDecimal(49)
                )
        );
    }

    @Test
    public void testUpdateOnlyById() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                1L, "SQL in Action", 1, new BigDecimal(45)
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setId(1L);
                            book.setName("SQL in Action");
                            book.setEdition(2);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ?",
                        "SQL in Action", 2, new BigDecimal(49), 1L
                )
        );
    }

    @Test
    public void testUpdateExistingDataByKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                1L, "SQL in Action", 1, new BigDecimal(45)
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 1L
                )
        );
    }

    //@Test
    public void testUpdateNonExistingDataByKey() {

        sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                )
        );
    }

    @Test
    public void testUpsertExistingDataById() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                1L, "SQL in Action", 1, new BigDecimal(45)
        );

        sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setId(1L);
                            book.setName("PL/SQL in Action");
                            book.setEdition(2);
                            //book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.ID = ?",
                        1L
                ),
                new ExecutedStatement(
                        "update BOOK " +
                                "set NAME = ?, EDITION = ? " +
                                "where ID = ?",
                        "PL/SQL in Action", 2, 1L
                )
        );
    }

    @Test
    public void testUpsertExistingDataByKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                1L, "SQL in Action", 1, new BigDecimal(45)
        );

        sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 1L
                )
        );
    }

    @Test
    public void testUpsertNonExistingDataById() {

        sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setId(1L);
                            book.setName("SQL in Action");
                            book.setEdition(2);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.ID = ?",
                        1L
                ),
                new ExecutedStatement(
                        "insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)",
                        1L, "SQL in Action", 2, new BigDecimal(49)
                )
        );
    }

    @Test
    public void testUpsertNonExistingDataByKey() {

        sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                        "SQL in Action", 1, new BigDecimal(49)
                )
        );
    }
}
