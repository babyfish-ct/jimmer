package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;
import org.babyfish.jimmer.sql.model.BookStoreDraft;
import org.babyfish.jimmer.sql.runtime.DbNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class SaveTest extends AbstractMutationTest {

    @Test
    public void testUpsertNotMatched() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store-> {
                            store.setId(newId);
                            store.setName("TURING");
                            store.setWebsite(null);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ where tb_1_.ID = ?"
                        );
                        it.variables(newId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, WEBSITE, VERSION) values(?, ?, ?, ?)");
                        it.variables(newId, "TURING", new DbNull(String.class), 0);
                    });
                }
        );
    }

    @Test
    public void upsertMatched() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(oreillyId);
                            store.setName("TURING");
                            store.setWebsite(null);
                            store.setVersion(0);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ where tb_1_.ID = ?"
                        );
                        it.variables(oreillyId);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE " +
                                "set NAME = ?, WEBSITE = ?, VERSION = VERSION + 1 " +
                                "where ID = ? and VERSION = ?"
                        );
                        it.variables("TURING", new DbNull(String.class), oreillyId, 0);
                    });
                }
        );
    }
}
