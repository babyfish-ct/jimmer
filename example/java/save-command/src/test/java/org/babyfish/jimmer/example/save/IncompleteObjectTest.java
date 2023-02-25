package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.BookStoreDraft;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.Test;

public class IncompleteObjectTest extends AbstractMutationTest {

    @Test
    public void testCompleteObject() {

        jdbc(
                "insert into book_store(id, name, website) values(?, ?, ?)",
                1, "O'REILLY", "http://www.oreilly.com"
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(1L);
                            store.setName("O'REILLY+");
                            store.setWebsite(null);
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "update BOOK_STORE " +
                                "set NAME = ?, WEBSITE = ? " +
                                "where ID = ?",
                        "O'REILLY+", null, 1L
                )
        );
    }

    @Test
    public void testIncompleteObject() {

        jdbc(
                "insert into book_store(id, name, website) values(?, ?, ?)",
                1, "O'REILLY", "http://www.oreilly.com"
        );

        sql()
                .getEntities()
                .saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(1L);
                            store.setName("O'REILLY+");
                        })
                )
                .configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY))
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "update BOOK_STORE " +
                                "set NAME = ? " +
                                "where ID = ?",
                        "O'REILLY+", 1L
                )
        );
    }
}
