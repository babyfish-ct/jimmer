package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.babyfish.jimmer.sql.common.Constants.*;

import java.math.BigDecimal;
import java.util.UUID;

public class DMLWithTriggerTest extends AbstractTriggerTest {

    @Test
    public void testUpdate() {

        BookTable book = BookTable.$;

        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(book.name().eq("GraphQL in Action")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("GraphQL in Action");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK tb_1_ " +
                                        "set PRICE = tb_1_.PRICE + ? " +
                                        "where tb_1_.ID in(?, ?, ?) " +
                                        "and tb_1_.NAME = ?"
                        );
                        it.unorderedVariables(
                                BigDecimal.ONE,
                                graphQLInActionId1, graphQLInActionId2, graphQLInActionId3,
                                "GraphQL in Action"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in(?, ?, ?)"
                        );
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.rowCount(3);
                }
        );
        assertEvents(
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
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
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
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + graphQLInActionId1 + "\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":81.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
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
                        "--->--->\"price\":82.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + manningId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpdateFailed() {
        AuthorTableEx author = AuthorTableEx.$;
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            getSqlClient().createUpdate(author)
                    .set(author.books().name(), author.books().name().concat("*"));
        });
        assertEvents();
        Assertions.assertEquals(
                "Only the primary table can be deleted when transaction trigger is supported",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateWithJoinByMySql() {

        BookTableEx book = BookTableEx.$;

        try {
            NativeDatabases.assumeNativeDatabase();
        } catch (TestAbortedException ex) {
            assertEvents();
            throw ex;
        }

        executeAndExpectRowCount(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new MySqlDialect());
                    it.addScalarProvider(ScalarProvider.UUID_BY_BYTE_ARRAY);
                })
                        .createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(book.authors().firstName().eq("Alex")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                        "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                        "where tb_3_.FIRST_NAME = ?"
                        );
                        it.variables("Alex");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                        "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                        "set tb_1_.PRICE = tb_1_.PRICE + ? " +
                                        "where tb_1_.ID in(?, ?, ?) " +
                                        "and tb_3_.FIRST_NAME = ?"
                        );
                        it.unorderedVariables(
                                BigDecimal.ONE,
                                toBytes(learningGraphQLId1),
                                toBytes(learningGraphQLId2),
                                toBytes(learningGraphQLId3),
                                "Alex"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in(?, ?, ?)"
                        );
                        it.unorderedVariables(
                                toBytes(learningGraphQLId1),
                                toBytes(learningGraphQLId2),
                                toBytes(learningGraphQLId3)
                        );
                    });
                    ctx.rowCount(3);
                }
        );

        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId3 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId3 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":52.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
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
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId2 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":56.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
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
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId1 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpdateWithJoinByPostgres() {

        BookTableEx book = BookTableEx.$;

        try {
            NativeDatabases.assumeNativeDatabase();
        } catch (TestAbortedException ex) {
            assertEvents();
            throw ex;
        }

        executeAndExpectRowCount(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                })
                        .createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(book.authors().firstName().eq("Alex")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                        "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                        "where tb_3_.FIRST_NAME = ?"
                        );
                        it.variables("Alex");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK tb_1_ set PRICE = tb_1_.PRICE + ? " +
                                        "from BOOK_AUTHOR_MAPPING as tb_2_ " +
                                        "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                        "where tb_1_.ID in(?, ?, ?) " +
                                        "and tb_1_.ID = tb_2_.BOOK_ID " +
                                        "and tb_3_.FIRST_NAME = ?"
                        );
                        it.unorderedVariables(
                                BigDecimal.ONE,
                                learningGraphQLId1, learningGraphQLId2, learningGraphQLId3,
                                "Alex"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in(?, ?, ?)"
                        );
                        it.unorderedVariables(
                                learningGraphQLId1, learningGraphQLId2, learningGraphQLId3
                        );
                    });
                    ctx.rowCount(3);
                }
        );

        assertEvents(
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
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId1 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
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
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId2 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":56.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId3 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"" + learningGraphQLId3 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":52.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testDelete() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createDelete(book)
                        .where(book.name().eq("GraphQL in Action")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select " +
                                        "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("GraphQL in Action");
                    });
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))");
                        it.unorderedVariables(
                                graphQLInActionId1, sammerId,
                                graphQLInActionId2, sammerId,
                                graphQLInActionId3, sammerId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?)");
                    });
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
                        "}"
        );
    }

    @Test
    public void testDeleteWithJoin() {
        BookTableEx book = BookTableEx.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createDelete(book)
                        .where(book.authors().firstName().eq("Alex")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                        "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                        "where tb_3_.FIRST_NAME = ?"
                        );
                        it.variables("Alex");
                    });
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID, AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID in(?, ?, ?)");
                        it.unorderedVariables(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?), (?, ?), (?, ?), (?, ?))"
                        );
                        it.unorderedVariables(
                                learningGraphQLId1, eveId,
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, eveId,
                                learningGraphQLId2, alexId,
                                learningGraphQLId3, eveId,
                                learningGraphQLId3, alexId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in(?, ?, ?)");
                        it.unorderedVariables(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3);
                    });
                }
        );
        assertEvents(
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
                        "--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->sourceId=" + learningGraphQLId3 + ", " +
                        "--->detachedTargetId=" + eveId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->sourceId=" + eveId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId3 + ", " +
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
                        "--->--->\"id\":\"" + learningGraphQLId3 + "\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"" + oreillyId + "\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=" + learningGraphQLId3 + ", " +
                        "--->detachedTargetId=" + oreillyId + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=" + oreillyId + ", " +
                        "--->detachedTargetId=" + learningGraphQLId3 + ", " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    private static byte[] toBytes(UUID uuid) {
        return ScalarProvider.UUID_BY_BYTE_ARRAY.toSql(uuid);
    }
}
