package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class DeleteDetachedObjectTest extends AbstractMutationTest {

    @Test
    public void testUpsertMatchedWithOneToManyAndDetachMode() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("O'REILLY");
                            store.setVersion(0);
                            store.addIntoBooks(book -> book.setId(learningGraphQLId1));
                            store.addIntoBooks(book -> book.setId(learningGraphQLId2));
                            store.addIntoBooks(book -> book.setId(learningGraphQLId3));
                        })
                ).configure(cfg ->
                        cfg.setDissociateAction(
                                BookProps.STORE,
                                DissociateAction.DELETE
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                        it.variables("O'REILLY");
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                        it.variables(oreillyId, 0);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = ? where ID in(?, ?, ?)");
                        it.variables(oreillyId, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("select ID from BOOK where STORE_ID = ? and ID not in(?, ?, ?) for update");
                        it.variables(oreillyId, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?, ?, ?, ?)");
                        it.unorderedVariables(
                                effectiveTypeScriptId1, effectiveTypeScriptId2, effectiveTypeScriptId3,
                                programmingTypeScriptId1, programmingTypeScriptId2, programmingTypeScriptId3
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?, ?, ?, ?)");
                        it.unorderedVariables(
                                effectiveTypeScriptId1, effectiveTypeScriptId2, effectiveTypeScriptId3,
                                programmingTypeScriptId1, programmingTypeScriptId2, programmingTypeScriptId3
                        );
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"O'REILLY\"," +
                                "\"version\":0," +
                                "\"books\":[" +
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                "\"name\":\"O'REILLY\"," +
                                "\"version\":1," +
                                "\"books\":[" +
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(16);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 9);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 6);
                }
        );
    }
}
