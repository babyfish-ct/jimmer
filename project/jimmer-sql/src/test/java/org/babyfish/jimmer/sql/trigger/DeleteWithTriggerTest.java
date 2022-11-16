package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class DeleteWithTriggerTest extends AbstractTriggerTest {

    @Test
    public void testDeleteBookStore() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID from BOOK where STORE_ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message(
                                "Cannot delete entities whose type are \"org.babyfish.jimmer.sql.model.BookStore\" " +
                                        "because there are some child entities whose type are \"org.babyfish.jimmer.sql.model.Book\", " +
                                        "these child entities use the association property \"org.babyfish.jimmer.sql.model.Book.store\" " +
                                        "to reference current entities."
                        );
                    });
                }
        );
        assertEvents();
    }

    @Test
    public void testDeleteBookStoreOnDissociateSetNull() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setDissociateAction(
                            BookProps.STORE,
                            DissociateAction.SET_NULL
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.STORE_ID = ?"
                        );
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where ID in(?, ?, ?)");
                        it.variables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.ID in (?) " +
                                        "for update"
                        );
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID in(?)");
                        it.variables(manningId);
                    });
                    ctx
                            .totalRowCount(4)
                            .rowCount(AffectedTable.of(Book.class), 3)
                            .rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId1 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId2 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId2 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId2 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + manningId + "\"," +
                        "--->--->\"name\":\"MANNING\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDeleteBookStoreOnDissociateDelete() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        BookStore.class,
                        manningId
                ).configure(cfg -> {
                    cfg.setDissociateAction(
                            BookProps.STORE,
                            DissociateAction.DELETE
                    );
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select ID from BOOK where STORE_ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.variables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.unorderedVariables(
                                graphQLInActionId1, sammerId,
                                graphQLInActionId2, sammerId,
                                graphQLInActionId3, sammerId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in (?, ?, ?) " +
                                        "for update"
                        );
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.ID in (?) " +
                                        "for update"
                        );
                        it.variables(manningId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_STORE where ID in(?)");
                        it.variables(manningId);
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 3);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 3);
                }
        );
        assertEvents(
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->detachedTargetId=" + sammerId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + sammerId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + graphQLInActionId1 + ", " +
                        "--->detachedTargetId=" + sammerId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + sammerId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + graphQLInActionId2 + ", " +
                        "--->detachedTargetId=" + sammerId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + sammerId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId1 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId2 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + graphQLInActionId2 + ", " +
                        "--->detachedTargetId=" + manningId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + manningId + ", " +
                        "--->detachedTargetId=" + graphQLInActionId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + manningId + "\"," +
                        "--->--->\"name\":\"MANNING\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testBook() {
        UUID nonExistingId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().batchDeleteCommand(
                        Book.class,
                        Arrays.asList(
                            learningGraphQLId1,
                            learningGraphQLId2,
                            nonExistingId
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?), (?, ?))");
                        it.unorderedVariables(
                                learningGraphQLId1, eveId, learningGraphQLId1, alexId,
                                learningGraphQLId2, eveId, learningGraphQLId2, alexId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in (?, ?, ?) " +
                                        "for update"
                        );
                        it.variables(learningGraphQLId1, learningGraphQLId2, nonExistingId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?)");
                        it.unorderedVariables(learningGraphQLId1, learningGraphQLId2);
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 4);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
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
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=" + eveId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + eveId + ", " +
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
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId1 + ", " +
                        "--->detachedTargetId=" + eveId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + eveId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId2 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":55.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + learningGraphQLId2 + ", " +
                        "--->detachedTargetId=" + oreillyId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + oreillyId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId2 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId1 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":50.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + learningGraphQLId1 + ", " +
                        "--->detachedTargetId=" + oreillyId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + oreillyId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId1 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDeleteAuthor() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Author.class,
                        alexId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select AUTHOR_ID, BOOK_ID from BOOK_AUTHOR_MAPPING where AUTHOR_ID in(?)");
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?), (?, ?))");
                        it.variables(alexId, learningGraphQLId1, alexId, learningGraphQLId2, alexId, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("select AUTHOR_ID, COUNTRY_CODE from AUTHOR_COUNTRY_MAPPING where AUTHOR_ID in(?)");
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR_COUNTRY_MAPPING where (AUTHOR_ID, COUNTRY_CODE) in ((?, ?))");
                        it.variables(alexId, "USA");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                        "from AUTHOR as tb_1_ " +
                                        "where tb_1_.ID in (?) " +
                                        "for update"
                        );
                        it.variables(alexId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from AUTHOR where ID in(?)");
                        it.variables(alexId);
                    });
                    ctx.totalRowCount(5);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.COUNTRY), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 3);
                }
        );
        assertEvents(
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
                        "}",
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
                        "--->sourceId=" + learningGraphQLId3 + ", " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId3 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.country, " +
                        "--->sourceId=" + alexId + ", " +
                        "--->detachedTargetId=USA, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Country.authors, " +
                        "--->sourceId=USA, " +
                        "--->detachedTargetId=" + alexId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + alexId + "\"," +
                        "--->--->\"firstName\":\"Alex\"," +
                        "--->--->\"lastName\":\"Banks\"," +
                        "--->--->\"gender\":\"MALE\"" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void deleteTree() {
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in(?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in(?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in(?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in(?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select NODE_ID from TREE_NODE where PARENT_ID in(?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE as tb_1_ " +
                                        "where tb_1_.NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                        "for update");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in(?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE as tb_1_ " +
                                        "where tb_1_.NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                        "for update");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in(?, ?, ?, ?, ?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE as tb_1_ " +
                                        "where tb_1_.NODE_ID in (?, ?, ?, ?) " +
                                        "for update");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in(?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE as tb_1_ " +
                                        "where tb_1_.NODE_ID in (?, ?) " +
                                        "for update");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in(?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE as tb_1_ " +
                                        "where tb_1_.NODE_ID in (?) " +
                                        "for update");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID in(?)");
                    });
                    ctx.totalRowCount(24);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":12," +
                        "--->--->\"name\":\"Dress\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":11" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=12, " +
                        "--->detachedTargetId=11, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=11, " +
                        "--->detachedTargetId=12, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":13," +
                        "--->--->\"name\":\"Miniskirt\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":11" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=13, " +
                        "--->detachedTargetId=11, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=11, " +
                        "--->detachedTargetId=13, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":14," +
                        "--->--->\"name\":\"Jeans\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":11" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=14, " +
                        "--->detachedTargetId=11, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=11, " +
                        "--->detachedTargetId=14, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":16," +
                        "--->--->\"name\":\"Suit\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":15" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=16, " +
                        "--->detachedTargetId=15, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=15, " +
                        "--->detachedTargetId=16, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":17," +
                        "--->--->\"name\":\"Shirt\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":15" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=17, " +
                        "--->detachedTargetId=15, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=15, " +
                        "--->detachedTargetId=17, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":20," +
                        "--->--->\"name\":\"Jacket\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":19" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=20, " +
                        "--->detachedTargetId=19, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=19, " +
                        "--->detachedTargetId=20, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":21," +
                        "--->--->\"name\":\"Jeans\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":19" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=21, " +
                        "--->detachedTargetId=19, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=19, " +
                        "--->detachedTargetId=21, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":23," +
                        "--->--->\"name\":\"Suit\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":22" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=23, " +
                        "--->detachedTargetId=22, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=22, " +
                        "--->detachedTargetId=23, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":24," +
                        "--->--->\"name\":\"Shirt\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":22" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=24, " +
                        "--->detachedTargetId=22, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=22, " +
                        "--->detachedTargetId=24, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":4," +
                        "--->--->\"name\":\"Coca Cola\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":3" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=4, " +
                        "--->detachedTargetId=3, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=3, " +
                        "--->detachedTargetId=4, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":5," +
                        "--->--->\"name\":\"Fanta\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":3" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=5, " +
                        "--->detachedTargetId=3, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=3, " +
                        "--->detachedTargetId=5, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":7," +
                        "--->--->\"name\":\"Baguette\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":6" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=7, " +
                        "--->detachedTargetId=6, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=6, " +
                        "--->detachedTargetId=7, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":8," +
                        "--->--->\"name\":\"Ciabatta\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":6" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=8, " +
                        "--->detachedTargetId=6, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=6, " +
                        "--->detachedTargetId=8, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":11," +
                        "--->--->\"name\":\"Casual wear\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":10" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=11, " +
                        "--->detachedTargetId=10, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=10, " +
                        "--->detachedTargetId=11, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":15," +
                        "--->--->\"name\":\"Formal wear\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":10" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=15, " +
                        "--->detachedTargetId=10, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=10, " +
                        "--->detachedTargetId=15, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":19," +
                        "--->--->\"name\":\"Casual wear\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":18" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=19, " +
                        "--->detachedTargetId=18, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=18, " +
                        "--->detachedTargetId=19, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":22," +
                        "--->--->\"name\":\"Formal wear\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":18" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=22, " +
                        "--->detachedTargetId=18, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=18, " +
                        "--->detachedTargetId=22, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":3," +
                        "--->--->\"name\":\"Drinks\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":2" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=3, " +
                        "--->detachedTargetId=2, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=2, " +
                        "--->detachedTargetId=3, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":6," +
                        "--->--->\"name\":\"Bread\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":2" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=6, " +
                        "--->detachedTargetId=2, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=2, " +
                        "--->detachedTargetId=6, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":10," +
                        "--->--->\"name\":\"Woman\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":9" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=10, " +
                        "--->detachedTargetId=9, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=9, " +
                        "--->detachedTargetId=10, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":18," +
                        "--->--->\"name\":\"Man\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":9" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=18, " +
                        "--->detachedTargetId=9, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=9, " +
                        "--->detachedTargetId=18, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":2," +
                        "--->--->\"name\":\"Food\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":1" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=2, " +
                        "--->detachedTargetId=1, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=1, " +
                        "--->detachedTargetId=2, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":9," +
                        "--->--->\"name\":\"Clothing\"," +
                        "--->--->\"parent\":{" +
                        "--->--->--->\"id\":1" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.parent, " +
                        "--->sourceId=9, " +
                        "--->detachedTargetId=1, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.TreeNode.childNodes, " +
                        "--->sourceId=1, " +
                        "--->detachedTargetId=9, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":1," +
                        "--->--->\"name\":\"Home\"," +
                        "--->--->\"parent\":null" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void deleteIllegalId() {
        UUID illegalId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        executeAndExpectResult(
                getSqlClient().getEntities().deleteCommand(
                        Book.class,
                        illegalId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in(?)");
                        it.variables(illegalId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in (?) " +
                                        "for update"
                        );
                        it.variables(illegalId);
                    });
                }
        );
        assertEvents();
    }
}
