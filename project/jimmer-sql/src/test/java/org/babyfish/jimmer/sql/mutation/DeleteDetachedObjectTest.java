package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
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
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.DELETE
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("O'REILLY");
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                        it.variables(oreillyId, 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where tb_1_.ID in (?, ?, ?)"
                        );
                        it.variables(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                        it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING tb_1_ " +
                                        "where exists (" +
                                        "--->select * " +
                                        "--->from BOOK tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.BOOK_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.STORE_ID = ? " +
                                        "--->and " +
                                        "--->--->tb_2_.ID not in (?, ?, ?)" +
                                        ")"
                        );
                        it.variables(oreillyId, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where STORE_ID = ? and ID not in (?, ?, ?)");
                        it.variables(oreillyId, learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                    ctx.totalRowCount(13);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 6);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 6);
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
                        it.modified(
                                "{" +
                                        "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "--->\"name\":\"O'REILLY\"," +
                                        "--->\"version\":1," +
                                        "--->\"books\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                        "--->--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                        "--->--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "--->--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }
}
