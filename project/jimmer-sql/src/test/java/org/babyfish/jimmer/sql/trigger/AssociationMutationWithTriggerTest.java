package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.model.AuthorProps;
import org.babyfish.jimmer.sql.model.BookProps;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class AssociationMutationWithTriggerTest extends AbstractTriggerTest {

    @Test
    public void testInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).saveCommand(
                       learningGraphQLId1, alexId
                ).checkExistence(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?))"
                        );
                        it.variables(learningGraphQLId1, alexId);
                    });
                    ctx.rowCount(0);
                }
        );
        assertEvents();
    }

    @Test
    public void testInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).batchSaveCommand(
                        Arrays.asList(
                            new Tuple2<>(learningGraphQLId1, alexId),
                            new Tuple2<>(learningGraphQLId2, borisId),
                            new Tuple2<>(learningGraphQLId3, borisId)
                        )
                ).checkExistence(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, borisId,
                                learningGraphQLId3, borisId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values (?, ?), (?, ?)"
                        );
                        it.variables(learningGraphQLId2, borisId, learningGraphQLId3, borisId);
                    });
                    ctx.rowCount(2);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + borisId + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + borisId + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + learningGraphQLId2 + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId3 + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + borisId + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + borisId + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + learningGraphQLId3 + ", " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(BookProps.AUTHORS).batchDeleteCommand(
                        Arrays.asList(
                            new Tuple2<>(learningGraphQLId1, alexId),
                            new Tuple2<>(learningGraphQLId2, alexId),
                            new Tuple2<>(learningGraphQLId3, borisId)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, alexId,
                                learningGraphQLId3, borisId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?))"
                        );
                        it.unorderedVariables(
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, alexId
                        );
                    });
                    ctx.rowCount(2);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId1 + ", " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testInverseInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).saveCommand(
                        alexId, learningGraphQLId1
                ).checkExistence(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select AUTHOR_ID, BOOK_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in ((?, ?))"
                        );
                        it.variables(alexId, learningGraphQLId1);
                    });
                    ctx.rowCount(0);
                }
        );
        assertEvents();
    }

    @Test
    public void testInverseInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).batchSaveCommand(
                        Arrays.asList(
                                new Tuple2<>(alexId, learningGraphQLId1),
                                new Tuple2<>(borisId, learningGraphQLId2),
                                new Tuple2<>(borisId, learningGraphQLId3)
                        )
                ).checkExistence(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select AUTHOR_ID, BOOK_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                alexId, learningGraphQLId1,
                                borisId, learningGraphQLId2,
                                borisId, learningGraphQLId3
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "values (?, ?), (?, ?)"
                        );
                        it.variables(borisId, learningGraphQLId2, borisId, learningGraphQLId3);
                    });
                    ctx.rowCount(2);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + borisId + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + borisId + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + learningGraphQLId2 + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId3 + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + borisId + ", " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + borisId + ", " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=" + learningGraphQLId3 + ", " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testInverseDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(AuthorProps.BOOKS).batchDeleteCommand(
                        Arrays.asList(
                                new Tuple2<>(alexId, learningGraphQLId1),
                                new Tuple2<>(alexId, learningGraphQLId2),
                                new Tuple2<>(borisId, learningGraphQLId3)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select AUTHOR_ID, BOOK_ID from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                alexId, learningGraphQLId1,
                                alexId, learningGraphQLId2,
                                borisId, learningGraphQLId3
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?))"
                        );
                        it.unorderedVariables(
                                alexId, learningGraphQLId1,
                                alexId, learningGraphQLId2
                        );
                    });
                    ctx.rowCount(2);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId1 + ", " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }
}
