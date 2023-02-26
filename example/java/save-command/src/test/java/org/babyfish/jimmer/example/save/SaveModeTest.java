package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.Book;
import org.babyfish.jimmer.example.save.model.BookDraft;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Recommended learning sequence: 1
 *
 * <p>[Current: SaveModeTest] -> IncompleteObjectTest -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest</p>
 */
public class SaveModeTest extends AbstractMutationTest {

    @Test
    public void testInsertOnly() {

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .setMode(SaveMode.INSERT_ONLY)
                .execute();

        // `INSERT_ONLY` represents direct insertion regardless of whether the data exists
        assertExecutedStatements(
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                        "SQL in Action",
                        1, new BigDecimal(49)
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());

        // `identity(10, 10)` in DDL
        Assertions.assertEquals(10L, result.getModifiedEntity().id());
    }

    @Test
    public void testUpdateOnlyById() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setId(10L);
                            book.setName("SQL in Action");
                            book.setEdition(2);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        // `UPDATE_ONLY` represents direct update regardless of whether the data exists
        assertExecutedStatements(
                new ExecutedStatement(
                        "update BOOK set NAME = ?, EDITION = ?, PRICE = ? where ID = ?",
                        "SQL in Action", 2, new BigDecimal(49), 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testUpdateExistingDataByKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        assertExecutedStatements(

                //Although `UPDATE_ONLY` is specified, the id attribute of the object is missing
                // so that it will still result in a key-based query.
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Update the selected data
                new ExecutedStatement(
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testUpdateNonExistingDataByKey() {

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                )
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        assertExecutedStatements(

                //Although `UPDATE_ONLY` is specified, the id attribute of the object is missing
                // so that it will still result in a key-based query.
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                )

                // No data can be selected, do nothing(affected row count is 0)
        );

        // Nothing updated
        Assertions.assertEquals(0, result.getTotalAffectedRowCount());
    }

    @Test
    public void testUpsertExistingDataById() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setId(10L);
                            book.setName("PL/SQL in Action");
                            book.setEdition(2);
                        })
                );

        assertExecutedStatements(

                // Query whether the data exists by id
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.ID = ?",
                        10L
                ),

                // Data exist, update it
                new ExecutedStatement(
                        "update BOOK " +
                                "set NAME = ?, EDITION = ? " +
                                "where ID = ?",
                        "PL/SQL in Action", 2, 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testUpsertExistingDataByKey() {

        jdbc(
                "insert into book(id, name, edition, price) values(?, ?, ?, ?)",
                10L, "SQL in Action", 1, new BigDecimal(45)
        );

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(

                // Query whether the data exists by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Data exists, update it
                new ExecutedStatement(
                        "update BOOK set PRICE = ? where ID = ?",
                        new BigDecimal(49), 10L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testUpsertNonExistingDataById() {

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setId(10L);
                            book.setName("SQL in Action");
                            book.setEdition(2);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(

                // Query whether the data exists by id
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.ID = ?",
                        10L
                ),

                // Data does not exists, insert it
                new ExecutedStatement(
                        "insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)",
                        10L, "SQL in Action", 2, new BigDecimal(49)
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
        Assertions.assertEquals(10L, result.getModifiedEntity().id());
    }

    @Test
    public void testUpsertNonExistingDataByKey() {

        SimpleSaveResult<Book> result = sql()
                .getEntities()
                .save(
                        BookDraft.$.produce(book -> {
                            book.setName("SQL in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(49));
                        })
                );

        assertExecutedStatements(

                // Query whether the data exists by key
                new ExecutedStatement(
                        "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK as tb_1_ where tb_1_.NAME = ? and tb_1_.EDITION = ?",
                        "SQL in Action", 1
                ),

                // Data does not exists, insert it
                new ExecutedStatement(
                        "insert into BOOK(NAME, EDITION, PRICE) values(?, ?, ?)",
                        "SQL in Action", 1, new BigDecimal(49)
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());

        // `identity(10, 10)` in DDL
        Assertions.assertEquals(10L, result.getModifiedEntity().id());
    }
}
