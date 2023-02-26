package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookStore;
import org.babyfish.jimmer.example.save.model.BookStoreDraft;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Recommended learning sequence: 2
 *
 * <p>SaveModeTest -> [Current: IncompleteObjectTest] -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> RecursiveTest -> TriggerTest</p>
 */
public class IncompleteObjectTest extends AbstractMutationTest {

    @Test
    public void testCompleteObject() {

        jdbc(
                "insert into book_store(id, name, website) values(?, ?, ?)",
                1, "O'REILLY", "http://www.oreilly.com"
        );

        SimpleSaveResult<BookStore> result = sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(1L);
                            store.setName("O'REILLY+");
                            store.setWebsite(null); // `website` is specified
                        })
                )
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        assertExecutedStatements(
                // `WEBSITE` is updated to be null
                new ExecutedStatement(
                        "update BOOK_STORE " +
                                "set NAME = ?, WEBSITE = ? " +
                                "where ID = ?",
                        "O'REILLY+", null, 1L
                )
        );

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }

    @Test
    public void testIncompleteObject() {

        jdbc(
                "insert into book_store(id, name, website) values(?, ?, ?)",
                1, "O'REILLY", "http://www.oreilly.com"
        );

        SimpleSaveResult<BookStore> result = sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(1L);
                            store.setName("O'REILLY+");

                            // `website` is not specified,
                            // this does NOT mean null, but UNKNOWN
                        })
                )
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        assertExecutedStatements(
                // Unspecified property `website` will not be updated
                new ExecutedStatement(
                        "update BOOK_STORE " +
                                "set NAME = ? " +
                                "where ID = ?",
                        "O'REILLY+", 1L
                )
        );

        /*
         * Objects can be incomplete, and unspecified properties will not be updated.
         *
         * This is a very important feature.
         *
         * - In traditional ORM, if you want to modify some properties of an object,
         *   you need to query the old object, modify the properties you want to modify,
         *   and finally save it.
         *
         * In Jimmer, create an object and specify the properties you want to modify,
         * save it.
         */

        Assertions.assertEquals(1, result.getTotalAffectedRowCount());
    }
}
