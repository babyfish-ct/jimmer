package org.babyfish.jimmer.sql.trigger;

import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.OptimisticLockException;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.runtime.DbNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class SaveWithTriggerTest extends AbstractTriggerTest {

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
                                        "from BOOK_STORE as tb_1_ where tb_1_.ID = ? " +
                                        "for update"
                        );
                        it.variables(newId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, WEBSITE, VERSION) values(?, ?, ?, ?)");
                        it.variables(newId, "TURING", new DbNull(String.class), 0);
                    });
                    ctx.entity(it -> {
                        it.original("{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"name\":\"TURING\",\"website\":null}");
                        it.modified("{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"name\":\"TURING\",\"website\":null,\"version\":0}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertMatched() {
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
                                        "from BOOK_STORE as tb_1_ where tb_1_.ID = ? " +
                                        "for update"
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
                    ctx.entity(it -> {
                        it.original("{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"TURING\",\"website\":null,\"version\":0}");
                        it.modified("{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"TURING\",\"website\":null,\"version\":1}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":1" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testInsert() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(newId);
                            store.setName("TURING");
                        })
                ).configure(cfg -> cfg.setMode(SaveMode.INSERT_ONLY)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(newId, "TURING", 0);
                    });
                    ctx.entity(it -> {
                        it.original("{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"name\":\"TURING\"}");
                        it.modified("{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"name\":\"TURING\",\"version\":0}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpdate() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setId(oreillyId);
                            store.setName("TURING");
                            store.setVersion(0);
                        })
                ).configure(cfg -> cfg.setMode(SaveMode.UPDATE_ONLY)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ where tb_1_.ID = ? " +
                                        "for update"
                        );
                        it.variables(oreillyId);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set NAME = ?, VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                        it.variables("TURING", oreillyId, 0);
                    });
                    ctx.entity(it -> {
                        it.original("{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"TURING\",\"version\":0}");
                        it.modified("{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"TURING\",\"version\":1}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"version\":1" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testInsertByKeyProps() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        setAutoIds(BookStore.class, newId);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("TURING");
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                        it.variables("TURING");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(newId, "TURING", 0);
                    });
                    ctx.entity(it -> {
                        it.original("{\"name\":\"TURING\"}");
                        it.modified("{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"name\":\"TURING\",\"version\":0}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"version\":0" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpdateByKeyProps() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("O'REILLY");
                            store.setWebsite("http://www.oreilly.com");
                            store.setVersion(0);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                        it.variables("O'REILLY");
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set WEBSITE = ?, VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                        it.variables("http://www.oreilly.com", oreillyId, 0);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"O'REILLY\"," +
                                "\"website\":\"http://www.oreilly.com\"," +
                                "\"version\":0" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                "\"name\":\"O'REILLY\"," +
                                "\"website\":\"http://www.oreilly.com\"," +
                                "\"version\":1" +
                                "}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"website\":\"http://www.oreilly.com\"," +
                        "--->--->\"version\":1" +
                        "--->}, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertNotMatchedWithManyToOne() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        setAutoIds(Book.class, newId);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Kotlin in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(30));
                            book.store(true).setId(manningId);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.EDITION = ? " +
                                        "for update"
                        );
                        it.variables("Kotlin in Action", 1);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)");
                        it.variables(newId, "Kotlin in Action", 1, new BigDecimal(30), manningId);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":30," +
                                "\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":30," +
                                "\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                "}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"Kotlin in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":30," +
                        "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertMatchedWithManyToOne() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Learning GraphQL");
                            book.setEdition(3);
                            book.store(true).setId(manningId);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.EDITION = ? " +
                                        "for update"
                        );
                        it.variables("Learning GraphQL", 3);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = ? where ID = ?");
                        it.variables(manningId, learningGraphQLId3);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"name\":\"Learning GraphQL\"," +
                                        "\"edition\":3," +
                                        "\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "\"name\":\"Learning GraphQL\"," +
                                        "\"edition\":3," +
                                        "\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                        "}");
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":51.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertNotMatchedWithOneToMany() {

        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        setAutoIds(BookStore.class, newId);

        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("TURING");
                            store.addIntoBooks(book -> book.setId(learningGraphQLId1));
                            store.addIntoBooks(book -> book.setId(learningGraphQLId2));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                        it.variables("TURING");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(newId, "TURING", 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in (?, ?) " +
                                        "for update");
                        it.unorderedVariables(learningGraphQLId1, learningGraphQLId2);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = ? where ID in(?, ?)");
                        it.unorderedVariables(newId, learningGraphQLId1, learningGraphQLId2);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"TURING\"," +
                                "\"books\":[" +
                                    "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                    "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                "\"name\":\"TURING\"," +
                                "\"version\":0," +
                                "\"books\":[" +
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"TURING\"," +
                        "--->--->\"version\":0," +
                        "--->--->\"books\":[" +
                        "--->--->--->{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                        "--->--->--->{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":55.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":55.00," +
                        "--->--->\"store\":{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=b649b11b-1161-4ad2-b261-af0112fdd7c8, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=b649b11b-1161-4ad2-b261-af0112fdd7c8, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=b649b11b-1161-4ad2-b261-af0112fdd7c8, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":50.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                        "--->--->\"name\":\"Learning GraphQL\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":50.00," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"" +
                        "--->--->}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=e110c564-23cc-4811-9e81-d587a13db634, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=e110c564-23cc-4811-9e81-d587a13db634, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=e110c564-23cc-4811-9e81-d587a13db634, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertMatchedWithOneToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("O'REILLY");
                            store.setVersion(0);
                            store.addIntoBooks(book -> book.setId(learningGraphQLId1));
                            store.addIntoBooks(book -> book.setId(learningGraphQLId2));
                            store.addIntoBooks(book -> book.setId(learningGraphQLId3));
                            store.addIntoBooks(book -> book.setId(graphQLInActionId1));
                        })
                ).configure(it ->
                        it.setDissociateAction(
                                BookProps.STORE,
                                DissociateAction.SET_NULL
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
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
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.ID in (?, ?, ?, ?) " +
                                        "for update"
                        );
                        it.unorderedVariables(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3, graphQLInActionId1);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = ? where ID in(?)");
                        it.variables(oreillyId, graphQLInActionId1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                        "from BOOK as tb_1_ " +
                                        "where tb_1_.STORE_ID = ? " +
                                        "and tb_1_.ID not in (?, ?, ?, ?)"
                        );
                        it.variables(list -> {
                            Assertions.assertEquals(oreillyId, list.get(0));
                            Assertions.assertTrue(list.contains(learningGraphQLId1));
                            Assertions.assertTrue(list.contains(learningGraphQLId2));
                            Assertions.assertTrue(list.contains(learningGraphQLId3));
                            Assertions.assertTrue(list.contains(graphQLInActionId1));
                        });
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where ID in(?, ?, ?, ?, ?, ?)");
                        it.unorderedVariables(
                                programmingTypeScriptId1,
                                programmingTypeScriptId2,
                                programmingTypeScriptId3,
                                effectiveTypeScriptId1,
                                effectiveTypeScriptId2,
                                effectiveTypeScriptId3
                        );
                    });

                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"O'REILLY\"," +
                                "\"version\":0," +
                                "\"books\":[" +
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}," +
                                "{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                "\"name\":\"O'REILLY\"," +
                                "\"version\":1," +
                                "\"books\":[" +
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                                "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}," +
                                "{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(8);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 7);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"website\":null," +
                        "--->--->\"version\":0" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                        "--->--->\"name\":\"O'REILLY\"," +
                        "--->--->\"version\":1," +
                        "--->--->\"books\":[" +
                        "--->--->--->{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}," +
                        "--->--->--->{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                        "--->--->--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}," +
                        "--->--->--->{\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                        "--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":80.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->detachedTargetId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->attachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=2fa3955e-3e83-49b9-902e-0465c109c779, " +
                        "--->detachedTargetId=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=null, " +
                        "--->attachedTargetId=a62f7aa3-9490-4612-98b5-98aae0e77120, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":73.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "}, " +
                        "newEntity={" +
                        "--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":73.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=8f30bc8a-49f9-481d-beca-5fe2d147c831, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=8f30bc8a-49f9-481d-beca-5fe2d147c831, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":69.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"8e169cfb-2373-4e44-8cce-1f1277f730d1\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":69.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=8e169cfb-2373-4e44-8cce-1f1277f730d1, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=8e169cfb-2373-4e44-8cce-1f1277f730d1, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":88.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                        "--->--->\"name\":\"Effective TypeScript\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":88.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"914c8595-35cb-4f67-bbc7-8029e9e6245a\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":47.50," +
                        "--->--->\"store\":{" +
                        "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                        "--->--->}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"914c8595-35cb-4f67-bbc7-8029e9e6245a\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":47.50," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=914c8595-35cb-4f67-bbc7-8029e9e6245a, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=914c8595-35cb-4f67-bbc7-8029e9e6245a, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":2,\"price\":45.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"058ecfd0-047b-4979-a7dc-46ee24d08f08\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":2," +
                        "--->--->\"price\":45.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=058ecfd0-047b-4979-a7dc-46ee24d08f08, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=058ecfd0-047b-4979-a7dc-46ee24d08f08, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "Event{" +
                        "--->oldEntity={" +
                        "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":48.00," +
                        "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                        "--->}, newEntity={" +
                        "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                        "--->--->\"name\":\"Programming TypeScript\"," +
                        "--->--->\"edition\":3," +
                        "--->--->\"price\":48.00," +
                        "--->--->\"store\":null" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->sourceId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, " +
                        "--->detachedTargetId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{" +
                        "--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->sourceId=d38c10da-6be8-4924-b9b9-5e81899612a0, " +
                        "--->detachedTargetId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, " +
                        "--->attachedTargetId=null, " +
                        "--->reason=null" +
                        "}"
        );
    }

    @Test
    public void testUpsertNotMatchedWithManyToMany() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        setAutoIds(Book.class, newId);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Kotlin in Action");
                            book.setEdition(1);
                            book.setPrice(new BigDecimal(30));
                            book.addIntoAuthors(author -> author.setId(danId));
                            book.addIntoAuthors(author -> author.setId(borisId));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? " +
                                "and tb_1_.EDITION = ? " +
                                "for update");
                        it.variables("Kotlin in Action", 1);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)");
                        it.variables(newId, "Kotlin in Action", 1, new BigDecimal(30));
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?), (?, ?)");
                        it.variables(newId, danId, newId, borisId);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":30," +
                                "\"authors\":[" +
                                "{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"}," +
                                "{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":30," +
                                "\"authors\":[" +
                                "{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"}," +
                                "{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                }
        );
        assertEvents(
                "Event{" +
                        "--->oldEntity=null, " +
                        "--->newEntity={" +
                        "--->--->\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                        "--->--->\"name\":\"Kotlin in Action\"," +
                        "--->--->\"edition\":1," +
                        "--->--->\"price\":30," +
                        "--->--->\"authors\":[" +
                        "--->--->--->{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"}," +
                        "--->--->--->{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"}" +
                        "--->--->]" +
                        "--->}, " +
                        "--->reason=null" +
                        "}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, detachedTargetId=null, attachedTargetId=c14665c8-c689-4ac7-b8cc-6f065b8d835d, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=c14665c8-c689-4ac7-b8cc-6f065b8d835d, detachedTargetId=null, attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, detachedTargetId=null, attachedTargetId=718795ad-77c1-4fcf-994a-fec6a5a11f0f, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=718795ad-77c1-4fcf-994a-fec6a5a11f0f, detachedTargetId=null, attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, reason=null}"
        );
    }

    @Test
    public void testUpsertMatchedWithManyToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Learning GraphQL");
                            book.setEdition(3);
                            book.addIntoAuthors(author -> author.setId(danId));
                            book.addIntoAuthors(author -> author.setId(borisId));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                "from BOOK as tb_1_ " +
                                "where tb_1_.NAME = ? " +
                                "and tb_1_.EDITION = ? " +
                                "for update");
                        it.variables("Learning GraphQL", 3);
                    });
                    ctx.statement(it -> {
                        it.sql("select AUTHOR_ID from BOOK_AUTHOR_MAPPING where BOOK_ID = ?");
                        it.variables(learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?))");
                        it.variables(learningGraphQLId3, alexId, learningGraphQLId3, eveId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values (?, ?), (?, ?)");
                        it.variables(learningGraphQLId3, danId, learningGraphQLId3, borisId);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"Learning GraphQL\"," +
                                "\"edition\":3," +
                                "\"authors\":[" +
                                "{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"}," +
                                "{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                "\"name\":\"Learning GraphQL\"," +
                                "\"edition\":3," +
                                "\"authors\":[" +
                                "{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"}," +
                                "{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 4);
                }
        );
        assertEvents(
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, detachedTargetId=1e93da94-af84-44f4-82d1-d8a9fd52ea94, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=1e93da94-af84-44f4-82d1-d8a9fd52ea94, detachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, detachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, detachedTargetId=null, attachedTargetId=c14665c8-c689-4ac7-b8cc-6f065b8d835d, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=c14665c8-c689-4ac7-b8cc-6f065b8d835d, detachedTargetId=null, attachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, detachedTargetId=null, attachedTargetId=718795ad-77c1-4fcf-994a-fec6a5a11f0f, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=718795ad-77c1-4fcf-994a-fec6a5a11f0f, detachedTargetId=null, attachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, reason=null}"
        );
    }

    @Test
    public void testUpsertNotMatchedWithInverseManyToMany() {

        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        setAutoIds(Author.class, newId);

        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        AuthorDraft.$.produce(author -> {
                            author.setFirstName("Jim");
                            author.setLastName("Green");
                            author.setGender(Gender.MALE);
                            author.addIntoBooks(book -> book.setId(effectiveTypeScriptId3));
                            author.addIntoBooks(book -> book.setId(programmingTypeScriptId3));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                        "from AUTHOR as tb_1_ " +
                                        "where tb_1_.FIRST_NAME = ? and " +
                                        "tb_1_.LAST_NAME = ? " +
                                        "for update"
                        );
                        it.variables("Jim", "Green");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)");
                        it.variables(newId, "Jim", "Green", "M");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) values (?, ?), (?, ?)");
                        it.variables(newId, effectiveTypeScriptId3, newId, programmingTypeScriptId3);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"firstName\":\"Jim\"," +
                                "\"lastName\":\"Green\"," +
                                "\"gender\":\"MALE\"," +
                                "\"books\":[" +
                                "{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}," +
                                "{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                "\"firstName\":\"Jim\"," +
                                "\"lastName\":\"Green\"," +
                                "\"gender\":\"MALE\"," +
                                "\"books\":[" +
                                "{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}," +
                                "{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 2);
                }
        );
        assertEvents(
                "Event{oldEntity=null, newEntity={\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\",\"firstName\":\"Jim\",\"lastName\":\"Green\",\"gender\":\"MALE\",\"books\":[{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"},{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}]}, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, detachedTargetId=null, attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, detachedTargetId=null, attachedTargetId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, detachedTargetId=null, attachedTargetId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=56506a3c-801b-4f7d-a41d-e889cdc3d67d, detachedTargetId=null, attachedTargetId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, reason=null}"
        );
    }

    @Test
    public void testUpsertMatchedWithInverseManyToMany() {

        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        AuthorDraft.$.produce(author -> {
                            author.setFirstName("Eve");
                            author.setLastName("Procello");
                            author.addIntoBooks(book -> book.setId(effectiveTypeScriptId3));
                            author.addIntoBooks(book -> book.setId(programmingTypeScriptId3));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                        "from AUTHOR as tb_1_ " +
                                        "where tb_1_.FIRST_NAME = ? and " +
                                        "tb_1_.LAST_NAME = ? " +
                                        "for update"
                        );
                        it.variables("Eve", "Procello");
                    });
                    ctx.statement(it -> {
                        it.sql("select BOOK_ID from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ?");
                        it.variables(eveId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?), (?, ?))");
                        it.variables(eveId, learningGraphQLId1, eveId, learningGraphQLId2, eveId, learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) values (?, ?), (?, ?)");
                        it.variables(eveId, effectiveTypeScriptId3, eveId, programmingTypeScriptId3);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"firstName\":\"Eve\"," +
                                "\"lastName\":\"Procello\"," +
                                "\"books\":[" +
                                "{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}," +
                                "{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                "]" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                "\"firstName\":\"Eve\"," +
                                "\"lastName\":\"Procello\"," +
                                "\"books\":[{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"}," +
                                "{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"}" +
                                "]" +
                                "}");
                    });
                    ctx.totalRowCount(5);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 5);
                }
        );
        assertEvents(
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=e110c564-23cc-4811-9e81-d587a13db634, detachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=e110c564-23cc-4811-9e81-d587a13db634, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=b649b11b-1161-4ad2-b261-af0112fdd7c8, detachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=b649b11b-1161-4ad2-b261-af0112fdd7c8, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=64873631-5d82-4bae-8eb8-72dd955bfc56, detachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=64873631-5d82-4bae-8eb8-72dd955bfc56, attachedTargetId=null, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, detachedTargetId=null, attachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=null, attachedTargetId=9eded40f-6d2e-41de-b4e7-33a28b11c8b6, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Book.authors, sourceId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, detachedTargetId=null, attachedTargetId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, reason=null}",
                "AssociationEvent{prop=org.babyfish.jimmer.sql.model.Author.books, sourceId=fd6bb6cf-336d-416c-8005-1ae11a6694b5, detachedTargetId=null, attachedTargetId=782b9a9d-eac8-41c4-9f2d-74a5d047f45a, reason=null}"
        );
    }

    @Test
    public void test() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("MANNING").setVersion(1);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                        "from BOOK_STORE as tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "for update"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE " +
                                        "set VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ?"
                        );
                    });
                    ctx.throwable(it -> {
                        it.message(
                                "Cannot update the entity whose " +
                                        "type is \"org.babyfish.jimmer.sql.model.BookStore\", " +
                                        "id is \"2fa3955e-3e83-49b9-902e-0465c109c779\" and " +
                                        "version is \"1\" at the path " +
                                        "\"<root>\""
                        );
                        it.type(OptimisticLockException.class);
                    });
                }
        );
    }
}
