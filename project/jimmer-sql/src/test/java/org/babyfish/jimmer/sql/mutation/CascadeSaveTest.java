package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.Department;
import org.babyfish.jimmer.sql.model.hr.DepartmentDraft;
import org.babyfish.jimmer.sql.model.hr.Employee;
import org.babyfish.jimmer.sql.model.inheritance.*;
import org.babyfish.jimmer.sql.model.wild.Task;
import org.babyfish.jimmer.sql.model.wild.Worker;
import org.babyfish.jimmer.sql.model.wild.WorkerDraft;
import org.babyfish.jimmer.sql.model.wild.WorkerProps;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CascadeSaveTest extends AbstractMutationTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testCascadeInsertWithManyToOne() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        UUID newStoreId = UUID.fromString("4749d255-2745-4f6b-99ae-61aa8fd463e0");
        setAutoIds(Book.class, newId);
        setAutoIds(BookStore.class, newStoreId);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Kotlin in Action").setEdition(1).setPrice(new BigDecimal(40));
                            book.store(true).setName("TURING").setWebsite("http://www.turing.com");
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ where tb_1_.NAME = ?"
                        );
                        it.variables("TURING");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, WEBSITE, VERSION) values(?, ?, ?, ?)");
                        it.variables(newStoreId, "TURING", "http://www.turing.com", 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("Kotlin in Action", 1);
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)"
                        );
                        it.variables(newId, "Kotlin in Action", 1, new BigDecimal(40), newStoreId);
                    });
                    ctx.entity(it -> {
                        it.original("{" +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":40," +
                                "\"store\":{\"name\":\"TURING\",\"website\":\"http://www.turing.com\"}" +
                                "}");
                        it.modified("{" +
                                "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                "\"name\":\"Kotlin in Action\"," +
                                "\"edition\":1," +
                                "\"price\":40," +
                                "\"store\":{" +
                                "\"id\":\"4749d255-2745-4f6b-99ae-61aa8fd463e0\"," +
                                "\"name\":\"TURING\"," +
                                "\"website\":\"http://www.turing.com\"," +
                                "\"version\":0" +
                                "}" +
                                "}");
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testCascadeUpdate() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book
                                    .setId(learningGraphQLId1).setPrice(new BigDecimal(40))
                                    .store(true)
                                    .setId(oreillyId).setWebsite("http://www.oreilly.com").setVersion(0);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.ID = ?"
                        );
                        it.variables(oreillyId);
                        it.queryReason(QueryReason.OPTIMISTIC_LOCK);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE " +
                                        "set WEBSITE = ?, VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ?"
                        );
                        it.variables("http://www.oreilly.com", oreillyId, 0);
                    });
                    ctx.statement(it -> {
                        it.sql("merge into BOOK(ID, PRICE, STORE_ID) key(ID) values(?, ?, ?)");
                        it.variables(learningGraphQLId1, new BigDecimal(40), oreillyId);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                        "\"price\":40," +
                                        "\"store\":{" +
                                        "\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "\"website\":\"http://www.oreilly.com\"," +
                                        "\"version\":0}" +
                                        "}"
                        );
                        it.modified(
                                "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                        "\"price\":40," +
                                        "\"store\":{" +
                                        "\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "\"website\":\"http://www.oreilly.com\"," +
                                        "\"version\":1}" +
                                        "}"
                        );
                        ctx.totalRowCount(2);
                        ctx.rowCount(AffectedTable.of(Book.class), 1);
                        ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    });
                }
        );
    }

    @Test
    public void testCascadeInsertWithOneToMany() {
        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        UUID newBookId1 = UUID.fromString("4749d255-2745-4f6b-99ae-61aa8fd463e0");
        UUID newBookId2 = UUID.fromString("4f351857-6cbc-4aad-ac3a-140a20034a3b");
        setAutoIds(BookStore.class, newId);
        setAutoIds(Book.class, newBookId1, newBookId2);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("TURING")
                                    .addIntoBooks(book -> {
                                        book.setName("SQL Cookbook").setEdition(1).setPrice(new BigDecimal(50));
                                    })
                                    .addIntoBooks(book -> {
                                        book.setName("Learning SQL").setEdition(1).setPrice(new BigDecimal(40));
                                    });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("TURING");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(newId, "TURING", 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables("SQL Cookbook", 1, "Learning SQL", 1);
                        it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)");
                        it.batchVariables(0, newBookId1, "SQL Cookbook", 1, new BigDecimal(50), newId);
                        it.batchVariables(1, newBookId2, "Learning SQL", 1, new BigDecimal(40), newId);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"name\":\"TURING\"," +
                                        "\"books\":[" +
                                        "{\"name\":\"SQL Cookbook\",\"edition\":1,\"price\":50}," +
                                        "{\"name\":\"Learning SQL\",\"edition\":1,\"price\":40}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                        "\"name\":\"TURING\"," +
                                        "\"version\":0," +
                                        "\"books\":[" +
                                        "{" +
                                        "\"id\":\"4749d255-2745-4f6b-99ae-61aa8fd463e0\"," +
                                        "\"name\":\"SQL Cookbook\"," +
                                        "\"edition\":1," +
                                        "\"price\":50," +
                                        "\"store\":{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"}" +
                                        "},{" +
                                        "\"id\":\"4f351857-6cbc-4aad-ac3a-140a20034a3b\"," +
                                        "\"name\":\"Learning SQL\"," +
                                        "\"edition\":1," +
                                        "\"price\":40," +
                                        "\"store\":{\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"}" +
                                        "}" +
                                        "]" +
                                        "}"
                        );
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testCascadeUpdateWithOneToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("O'REILLY").setVersion(0)
                                    .addIntoBooks(book -> {
                                        book.setName("Learning GraphQL")
                                                .setEdition(3).setPrice(new BigDecimal(45));
                                    })
                                    .addIntoBooks(book -> {
                                        book.setName("GraphQL in Action")
                                                .setEdition(3).setPrice(new BigDecimal(42));
                                    });
                        })
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.SET_NULL
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ " +
                                        "where tb_1_.NAME = ?"
                        );
                        it.variables("O'REILLY");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set VERSION = VERSION + 1 where ID = ? and VERSION = ?");
                        it.variables(oreillyId, 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables("Learning GraphQL", 3, "GraphQL in Action", 3);
                        it.queryReason(QueryReason.TARGET_NOT_TRANSFERABLE);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set PRICE = ?, STORE_ID = ? where ID = ?");
                        it.batchVariables(0, new BigDecimal(45), oreillyId, learningGraphQLId3);
                        it.batchVariables(1, new BigDecimal(42), oreillyId, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set STORE_ID = null where STORE_ID = ? and ID not in (?, ?)");
                        it.variables(oreillyId, learningGraphQLId3, graphQLInActionId3);
                    });
                    ctx.totalRowCount(11);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 10);
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"name\":\"O'REILLY\"," +
                                        "\"version\":0," +
                                        "\"books\":[" +
                                        "{\"name\":\"Learning GraphQL\",\"edition\":3,\"price\":45}," +
                                        "{\"name\":\"GraphQL in Action\",\"edition\":3,\"price\":42}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "\"name\":\"O'REILLY\"," +
                                        "\"version\":1," +
                                        "\"books\":[" +
                                        "{" +
                                        "\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "\"name\":\"Learning GraphQL\"," +
                                        "\"edition\":3," +
                                        "\"price\":45," +
                                        "\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "},{" +
                                        "\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "\"name\":\"GraphQL in Action\"," +
                                        "\"edition\":3," +
                                        "\"price\":42," +
                                        "\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                        "}" +
                                        "]" +
                                        "}");
                    });
                    ctx.totalRowCount(11);
                    ctx.rowCount(AffectedTable.of(Book.class), 10);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testCascadeUpdateWithEmptyOneToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(store -> {
                            store.setName("MANNING").setVersion(0)
                                    .setBooks(Collections.emptyList());
                        })
                ).setDissociateAction(
                        BookProps.STORE,
                        DissociateAction.SET_NULL
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ where tb_1_.NAME = ?"
                        );
                        it.variables("MANNING");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK_STORE " +
                                        "set VERSION = VERSION + 1 " +
                                        "where ID = ? and VERSION = ?"
                        );
                        it.variables(manningId, 0);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK set STORE_ID = null " +
                                        "where STORE_ID = ?"
                        );
                        it.variables(manningId);
                    });
                    ctx.entity(it -> {});
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Book.class), 3);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                }
        );
    }

    @Test
    public void testCascadeInsertWithManyToMany() {

        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        UUID newAuthorId1 = UUID.fromString("4749d255-2745-4f6b-99ae-61aa8fd463e0");
        UUID newAuthorId2 = UUID.fromString("4f351857-6cbc-4aad-ac3a-140a20034a3b");
        setAutoIds(Book.class, newId);
        setAutoIds(Author.class, newAuthorId1, newAuthorId2);

        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Kotlin in Action").setPrice(new BigDecimal(49)).setEdition(1)
                                    .addIntoAuthors(author -> {
                                        author.setFirstName("Andrey").setLastName("Breslav").setGender(Gender.MALE);
                                    })
                                    .addIntoAuthors(author -> {
                                        author.setFirstName("Pierre-Yves").setLastName("Saumont").setGender(Gender.MALE);
                                    });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("Kotlin in Action", 1);
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)");
                        it.variables(newId, "Kotlin in Action", 1, new BigDecimal(49));
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("Andrey", "Breslav", "Pierre-Yves", "Saumont");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, newAuthorId1, "Andrey", "Breslav", "M");
                        it.batchVariables(1, newAuthorId2, "Pierre-Yves", "Saumont", "M");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?)");
                        it.batchVariables(0, newId, newAuthorId1);
                        it.batchVariables(1, newId, newAuthorId2);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"name\":\"Kotlin in Action\"," +
                                        "\"edition\":1," +
                                        "\"price\":49," +
                                        "\"authors\":[" +
                                        "{\"firstName\":\"Andrey\",\"lastName\":\"Breslav\",\"gender\":\"MALE\"}," +
                                        "{\"firstName\":\"Pierre-Yves\",\"lastName\":\"Saumont\",\"gender\":\"MALE\"}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                        "\"name\":\"Kotlin in Action\"," +
                                        "\"edition\":1," +
                                        "\"price\":49," +
                                        "\"authors\":[" +
                                        "{\"id\":\"4749d255-2745-4f6b-99ae-61aa8fd463e0\"," +
                                        "\"firstName\":\"Andrey\",\"lastName\":\"Breslav\",\"gender\":\"MALE\"}," +
                                        "{\"id\":\"4f351857-6cbc-4aad-ac3a-140a20034a3b\"," +
                                        "\"firstName\":\"Pierre-Yves\",\"lastName\":\"Saumont\",\"gender\":\"MALE\"}" +
                                        "]" +
                                        "}"
                        );
                        ctx.totalRowCount(5);
                        ctx.rowCount(AffectedTable.of(Book.class), 1);
                        ctx.rowCount(AffectedTable.of(Author.class), 2);
                        ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                    });
                }
        );
    }

    @Test
    public void testCascadeUpdateWithManyToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Learning GraphQL").setPrice(new BigDecimal(49)).setEdition(3)
                                    .addIntoAuthors(author -> {
                                        author.setFirstName("Dan").setLastName("Vanderkam").setGender(Gender.FEMALE);
                                    })
                                    .addIntoAuthors(author -> {
                                        author.setFirstName("Boris").setLastName("Cherny").setGender(Gender.FEMALE);
                                    });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("Learning GraphQL", 3);
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set PRICE = ? where ID = ?");
                        it.variables(new BigDecimal(49), learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) in ((?, ?), (?, ?))"
                        );
                        it.variables("Dan", "Vanderkam", "Boris", "Cherny");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("update AUTHOR set GENDER = ? where ID = ?");
                        it.batchVariables(0, "F", danId);
                        it.batchVariables(1, "F", borisId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID not in (?, ?)");
                        it.variables(learningGraphQLId3, danId, borisId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "key(BOOK_ID, AUTHOR_ID) values(?, ?)"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"name\":\"Learning GraphQL\",\"edition\":3,\"price\":49," +
                                        "\"authors\":[" +
                                        "{\"firstName\":\"Dan\",\"lastName\":\"Vanderkam\",\"gender\":\"FEMALE\"}," +
                                        "{\"firstName\":\"Boris\",\"lastName\":\"Cherny\",\"gender\":\"FEMALE\"}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "\"name\":\"Learning GraphQL\",\"edition\":3,\"price\":49," +
                                        "\"authors\":[" +
                                        "{\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                                        "\"firstName\":\"Dan\",\"lastName\":\"Vanderkam\",\"gender\":\"FEMALE\"}," +
                                        "{\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"," +
                                        "\"firstName\":\"Boris\",\"lastName\":\"Cherny\",\"gender\":\"FEMALE\"}" +
                                        "]" +
                                        "}"
                        );
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(Author.class), 2);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 4);
                }
        );
    }

    @Test
    public void testCascadeUpdateWithEmptyManyToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        BookDraft.$.produce(book -> {
                            book.setName("Learning GraphQL").setPrice(new BigDecimal(49)).setEdition(3)
                                    .setAuthors(Collections.emptyList());
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) = (?, ?)"
                        );
                        it.variables("Learning GraphQL", 3);
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set PRICE = ? where ID = ?");
                        it.variables(new BigDecimal(49), learningGraphQLId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ?"
                        );
                        it.variables(learningGraphQLId3);
                    });
                    ctx.entity(it -> {});
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                }
        );
    }

    @Test
    public void testCascadeInsertWithInverseManyToMany() {

        UUID newId = UUID.fromString("56506a3c-801b-4f7d-a41d-e889cdc3d67d");
        UUID newBookId1 = UUID.fromString("4749d255-2745-4f6b-99ae-61aa8fd463e0");
        UUID newBookId2 = UUID.fromString("4f351857-6cbc-4aad-ac3a-140a20034a3b");
        setAutoIds(Author.class, newId);
        setAutoIds(Book.class, newBookId1, newBookId2);

        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        AuthorDraft.$.produce(author -> {
                            author.setFirstName("Jim").setLastName("Green").setGender(Gender.MALE)
                                    .addIntoBooks(book -> {
                                        book.setName("Learning SQL").setEdition(1).setPrice(new BigDecimal(30));
                                    })
                                    .addIntoBooks(book -> {
                                        book.setName("SQL Cookbook").setEdition(1).setPrice(new BigDecimal(40));
                                    });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) = (?, ?)"
                        );
                        it.variables("Jim", "Green");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?)");
                        it.variables(newId, "Jim", "Green", "M");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables("Learning SQL", 1, "SQL Cookbook", 1);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?)"
                        );
                        it.batchVariables(0, newBookId1, "Learning SQL", 1, new BigDecimal(30));
                        it.batchVariables(1, newBookId2, "SQL Cookbook", 1, new BigDecimal(40));
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) values(?, ?)");
                        it.batchVariables(0, newId, newBookId1);
                        it.batchVariables(1, newId, newBookId2);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"firstName\":\"Jim\",\"lastName\":\"Green\",\"gender\":\"MALE\"," +
                                        "\"books\":[" +
                                        "{\"name\":\"Learning SQL\",\"edition\":1,\"price\":30}," +
                                        "{\"name\":\"SQL Cookbook\",\"edition\":1,\"price\":40}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"56506a3c-801b-4f7d-a41d-e889cdc3d67d\"," +
                                        "\"firstName\":\"Jim\",\"lastName\":\"Green\",\"gender\":\"MALE\"," +
                                        "\"books\":[" +
                                        "{\"id\":\"4749d255-2745-4f6b-99ae-61aa8fd463e0\"," +
                                        "\"name\":\"Learning SQL\",\"edition\":1,\"price\":30}," +
                                        "{\"id\":\"4f351857-6cbc-4aad-ac3a-140a20034a3b\"," +
                                        "\"name\":\"SQL Cookbook\",\"edition\":1,\"price\":40}" +
                                        "]" +
                                        "}"
                        );
                        ctx.totalRowCount(5);
                        ctx.rowCount(AffectedTable.of(Book.class), 2);
                        ctx.rowCount(AffectedTable.of(Author.class), 1);
                        ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 2);
                    });
                }
        );
    }

    @Test
    public void testCascadeUpdateWithInverseManyToMany() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        AuthorDraft.$.produce(author ->{
                            author.setFirstName("Eve").setLastName("Procello").setGender(Gender.FEMALE)
                                    .addIntoBooks(book -> {
                                        book.setName("Learning GraphQL").setEdition(3).setPrice(new BigDecimal(35));
                                    })
                                    .addIntoBooks(book -> {
                                        book.setName("GraphQL in Action").setEdition(3).setPrice(new BigDecimal(28));
                                    });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                        "from AUTHOR tb_1_ " +
                                        "where (tb_1_.FIRST_NAME, tb_1_.LAST_NAME) = (?, ?)"
                        );
                        it.variables("Eve", "Procello");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update AUTHOR set GENDER = ? where ID = ?"
                        );
                        it.variables("F", eveId);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                        it.variables("Learning GraphQL", 3, "GraphQL in Action", 3);
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("update BOOK set PRICE = ? where ID = ?");
                        it.batchVariables(0, new BigDecimal(35), learningGraphQLId3);
                        it.batchVariables(1, new BigDecimal(28), graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where AUTHOR_ID = ? and BOOK_ID not in (?, ?)");
                        it.variables(eveId, learningGraphQLId3, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "key(AUTHOR_ID, BOOK_ID) values(?, ?)"
                        );
                        it.batchVariables(0, eveId, learningGraphQLId3);
                        it.batchVariables(1, eveId, graphQLInActionId3);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "\"firstName\":\"Eve\",\"lastName\":\"Procello\",\"gender\":\"FEMALE\"," +
                                        "\"books\":[" +
                                        "{\"name\":\"Learning GraphQL\",\"edition\":3,\"price\":35}," +
                                        "{\"name\":\"GraphQL in Action\",\"edition\":3,\"price\":28}" +
                                        "]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                        "\"firstName\":\"Eve\",\"lastName\":\"Procello\",\"gender\":\"FEMALE\"," +
                                        "\"books\":[" +
                                        "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "\"name\":\"Learning GraphQL\",\"edition\":3,\"price\":35" +
                                        "},{" +
                                        "\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "\"name\":\"GraphQL in Action\"," +
                                        "\"edition\":3," +
                                        "\"price\":28" +
                                        "}" +
                                        "]" +
                                        "}"
                        );
                    });
                    ctx.totalRowCount(7);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 4);
                }
        );
    }

    @Test
    public void testCascadeInsertWithOneToOne() {
        setAutoIds(Administrator.class, 5L);
        setAutoIds(AdministratorMetadata.class, 50L);
        executeAndExpectResult(
                getSqlClient(it -> {
                    UserIdGenerator idGenerator = this::autoId;
                    it.setIdGenerator(idGenerator);
                    it.addDraftInterceptor(new Interceptor());
                }).getEntities().saveCommand(
                        AdministratorDraft.$.produce(draft -> {
                            draft.setName("a_5");
                            draft.applyMetadata(metadata -> {
                                metadata.setName("am_5");
                                metadata.setEmail("email_5");
                                metadata.setWebsite("website_5");
                            });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME from " +
                                        "ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED <> ?"
                        );
                        it.variables("a_5", true);
                        it.queryReason(QueryReason.INTERCEPTOR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                5L,
                                "a_5",
                                false,
                                Interceptor.TIMESTAMP,
                                Interceptor.TIMESTAMP
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ADMINISTRATOR_METADATA tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED <> ?"
                        );
                        it.variables("am_5", true);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR_METADATA(" +
                                        "--->ID, " +
                                        "--->NAME, DELETED, CREATED_TIME, MODIFIED_TIME, EMAIL, WEBSITE, ADMINISTRATOR_ID" +
                                        ") values(?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                50L,
                                "am_5",
                                false,
                                Interceptor.TIMESTAMP,
                                Interceptor.TIMESTAMP,
                                "email_5",
                                "website_5",
                                5L
                        );
                    });
                    ctx.totalRowCount(2);
                    ctx.rowCount(AffectedTable.of(Administrator.class), 1);
                    ctx.rowCount(AffectedTable.of(AdministratorMetadata.class), 1);
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"a_5\"," +
                                        "--->\"metadata\":{" +
                                        "--->--->\"name\":\"am_5\"," +
                                        "--->--->\"email\":\"email_5\"," +
                                        "--->--->\"website\":\"website_5\"" +
                                        "--->}" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"name\":\"a_5\"," +
                                        "--->\"deleted\":false," +
                                        "--->\"createdTime\":\"2022-10-15 16:55:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-15 16:55:00\"," +
                                        "--->\"metadata\":{" +
                                        "--->--->\"name\":\"am_5\"," +
                                        "--->--->\"deleted\":false," +
                                        "--->--->\"createdTime\":\"2022-10-15 16:55:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-15 16:55:00\"," +
                                        "--->--->\"email\":\"email_5\"," +
                                        "--->--->\"website\":\"website_5\"," +
                                        "--->--->\"administrator\":{\"id\":5}," +
                                        "--->--->\"id\":50" +
                                        "--->}," +
                                        "--->\"id\":5" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testCascadeInsertWithLogicalDeletedAndOneToOne() {
        setAutoIds(Administrator.class, 10001L);
        setAutoIds(AdministratorMetadata.class, 10010L);
        executeAndExpectResult(
                getSqlClient(it -> {
                    UserIdGenerator<?> idGenerator = this::autoId;
                    it.setIdGenerator(idGenerator);
                    it.addDraftInterceptor(new Interceptor());
                }).getEntities().saveCommand(
                        AdministratorDraft.$.produce(draft -> {
                            draft.setName("a_4");
                            draft.applyMetadata(metadata -> {
                                metadata.setName("am_4");
                                metadata.setEmail("email_4+");
                                metadata.setWebsite("website_4+");
                            });
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED <> ?"
                        );
                        it.variables("a_4", true);
                        it.queryReason(QueryReason.INTERCEPTOR);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                10001L,
                                "a_4",
                                false,
                                Timestamp.valueOf("2022-10-15 16:55:00"),
                                Timestamp.valueOf("2022-10-15 16:55:00")
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from ADMINISTRATOR_METADATA tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED <> ?"
                        );
                        it.variables("am_4", true);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR_METADATA(" +
                                        "--->ID, " +
                                        "--->NAME, DELETED, CREATED_TIME, MODIFIED_TIME, EMAIL, WEBSITE, ADMINISTRATOR_ID" +
                                        ") values(?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                10010L,
                                "am_4",
                                false,
                                Timestamp.valueOf("2022-10-15 16:55:00"),
                                Timestamp.valueOf("2022-10-15 16:55:00"),
                                "email_4+",
                                "website_4+",
                                10001L
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"a_4\"," +
                                        "--->\"metadata\":{" +
                                        "--->--->\"name\":\"am_4\"," +
                                        "--->--->\"email\":\"email_4+\"," +
                                        "--->--->\"website\":\"website_4+\"" +
                                        "--->}" +
                                        "}"
                        );
                        ctx.totalRowCount(2);
                        ctx.rowCount(AffectedTable.of(Administrator.class), 1);
                        ctx.rowCount(AffectedTable.of(AdministratorMetadata.class), 1);
                        it.modified(
                                "{" +
                                        "--->\"name\":\"a_4\"," +
                                        "--->\"deleted\":false," +
                                        "--->\"createdTime\":\"2022-10-15 16:55:00\"," +
                                        "--->\"modifiedTime\":\"2022-10-15 16:55:00\"," +
                                        "--->\"metadata\":{" +
                                        "--->--->\"name\":\"am_4\"," +
                                        "--->--->\"deleted\":false," +
                                        "--->--->\"createdTime\":\"2022-10-15 16:55:00\"," +
                                        "--->--->\"modifiedTime\":\"2022-10-15 16:55:00\"," +
                                        "--->--->\"email\":\"email_4+\"," +
                                        "--->--->\"website\":\"website_4+\"," +
                                        "--->--->\"administrator\":{\"id\":10001}," +
                                        "--->--->\"id\":10010" +
                                        "--->}," +
                                        "--->\"id\":10001" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void saveTree() {
        setAutoIds(TreeNode.class, 100L, 101L, 102L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        TreeNodeDraft.$.produce(treeNode ->
                                treeNode
                                        .setName("Parent")
                                        .setParent(null)
                                        .addIntoChildNodes(child ->
                                                child.setName("Child-1")
                                        )
                                        .addIntoChildNodes(child ->
                                                child.setName("Child-2")
                                        )
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID is null and tb_1_.NAME = ?"
                        );
                        it.variables("Parent");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.variables(100L, "Parent", new DbLiteral.DbNull(long.class));
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.PARENT_ID) in ((?, ?), (?, ?))"
                        );
                        it.variables("Child-1", 100L, "Child-2", 100L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.batchVariables(0, 101L, "Child-1", 100L);
                        it.batchVariables(1, 102L, "Child-2", 100L);
                    });
                    ctx.rowCount(AffectedTable.of(TreeNode.class), 3);
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"Parent\"," +
                                        "--->\"parent\":null," +
                                        "--->\"childNodes\":[" +
                                        "--->--->{\"name\":\"Child-1\"}," +
                                        "--->--->{\"name\":\"Child-2\"}" +
                                        "--->]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":100,\"name\":" +
                                        "--->\"Parent\",\"" +
                                        "--->parent\":null," +
                                        "--->\"childNodes\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":101," +
                                        "--->--->--->\"name\":\"Child-1\"," +
                                        "--->--->--->\"parent\":{\"id\":100}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":102," +
                                        "--->--->--->\"name\":\"Child-2\"," +
                                        "--->--->--->\"parent\":{\"id\":100}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testSaveAll() {
        UUID storeId = UUID.fromString("6057bec2-df8d-48f1-b31e-fdd36861cccb");
        UUID bookId1 = UUID.fromString("e1d34561-17df-4c08-9959-1c6cb33cdafb");
        UUID bookId2 = UUID.fromString("3d1d7676-5258-41c0-8e21-02110af07e90");
        setAutoIds(Book.class, bookId1, bookId2);

        BookStore store = BookStoreDraft.$.produce(draft -> {
            draft.setId(storeId);
            draft.setName("TURING");
            draft.setVersion(0);
        });
        List<Book> books = Arrays.asList(
                BookDraft.$.produce(book -> {
                    book.setName("A")
                            .setEdition(1)
                            .setPrice(new BigDecimal(48))
                            .setStore(store);
                }),
                BookDraft.$.produce(book -> {
                    book.setName("B")
                            .setEdition(1)
                            .setPrice(new BigDecimal(49))
                            .setStore(store);
                })
        );
        executeAndExpectResult(
                getSqlClient().getEntities().saveAllCommand(books),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from BOOK_STORE tb_1_ where tb_1_.ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                        "from BOOK tb_1_ " +
                                        "where (tb_1_.NAME, tb_1_.EDITION) in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                        it.batches(2);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"A\",\"edition\":1,\"price\":48,\"store\":{\"id\":\"6057bec2-df8d-48f1-b31e-fdd36861cccb\",\"name\":\"TURING\",\"version\":0}}"
                        );
                        it.modified(
                                "{\"id\":\"e1d34561-17df-4c08-9959-1c6cb33cdafb\",\"name\":\"A\",\"edition\":1,\"price\":48,\"store\":{\"id\":\"6057bec2-df8d-48f1-b31e-fdd36861cccb\",\"name\":\"TURING\",\"version\":0}}"
                        );
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"B\",\"edition\":1,\"price\":49,\"store\":{\"id\":\"6057bec2-df8d-48f1-b31e-fdd36861cccb\",\"name\":\"TURING\",\"version\":0}}"
                        );
                        it.modified(
                                "{\"id\":\"3d1d7676-5258-41c0-8e21-02110af07e90\",\"name\":\"B\",\"edition\":1,\"price\":49,\"store\":{\"id\":\"6057bec2-df8d-48f1-b31e-fdd36861cccb\",\"name\":\"TURING\",\"version\":0}}"
                        );
                    });
                }
        );
    }

    private static class Interceptor implements DraftInterceptor<NamedEntity, NamedEntityDraft> {

        private static final LocalDateTime TIME = LocalDateTime.of(
                2022, 10, 15, 16, 55
        );

        static final Timestamp TIMESTAMP = Timestamp.valueOf(TIME);

        @Override
        public void beforeSave(@NotNull NamedEntityDraft draft, NamedEntity original) {
            if (!ImmutableObjects.isLoaded(draft, NamedEntityProps.DELETED)) {
                draft.setDeleted(false);
            }
            if (!ImmutableObjects.isLoaded(draft, NamedEntityProps.MODIFIED_TIME)) {
                draft.setModifiedTime(TIME);
            }
            if (original == null && !ImmutableObjects.isLoaded(draft, NamedEntityProps.CREATED_TIME)) {
                draft.setCreatedTime(TIME);
            }
        }
    }

    @Test
    public void testAppendOnlyChild() {
        setAutoIds(Employee.class, 100L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        DepartmentDraft.$.produce(draft -> {
                            draft.setId(1L);
                            draft.setName("Develop");
                            draft.addIntoEmployees(employee -> employee.setName("Tim"));
                        })
                ).setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into DEPARTMENT(ID, NAME, DELETED_MILLIS) key(ID) values(?, ?, ?)");
                        it.variables(1L, "Develop", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(ID, NAME, DELETED_MILLIS, DEPARTMENT_ID) values(?, ?, ?, ?)");
                        it.variables(100L, "Tim", 0L, 1L);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"id\":\"1\",\"name\":\"Develop\",\"employees\":[{\"name\":\"Tim\"}]}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Tim\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}");
                    });
                }
        );
    }

    @Test
    public void testAppendOnlyAll() {
        setAutoIds(Department.class, 10L);
        setAutoIds(Employee.class, 100L);
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        DepartmentDraft.$.produce(draft -> {
                            draft.setName("Develop");
                            draft.addIntoEmployees(employee -> employee.setName("Tim"));
                        })
                ).setAssociatedModeAll(AssociatedSaveMode.APPEND).setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into DEPARTMENT(ID, NAME, DELETED_MILLIS) values(?, ?, ?)");
                        it.variables(10L, "Develop", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into EMPLOYEE(ID, NAME, DELETED_MILLIS, DEPARTMENT_ID) values(?, ?, ?, ?)");
                        it.variables(100L, "Tim", 0L, 10L);
                    });
                    ctx.entity(it -> {
                        it.original(
                                "{\"name\":\"Develop\",\"employees\":[{\"name\":\"Tim\"}]}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"10\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"100\"," +
                                        "--->--->--->\"name\":\"Tim\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"10\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}");
                    });
                }
        );
    }

    @Test
    public void testAppendOnlyByIgnoringKey() {
        setAutoIds(BookStore.class, UUID.fromString("bdd0445b-c71f-46fd-80ed-b646d32b1351"));
        setAutoIds(
                Book.class,
                UUID.fromString("8b16a8cf-cb8a-4781-87b8-652dc7a1d04f"),
                UUID.fromString("003b0ef9-f63f-480f-b752-348a23c00997")
        );
        BookStore store = Objects.createBookStore(draft -> {
            draft.setName("TURING");
            draft.setWebsite("https://www.turing.org");
            draft.addIntoBooks(book -> {
                book.setName("SQL Optimization");
                book.setEdition(1);
                book.setPrice(new BigDecimal("59.99"));
            });
            draft.addIntoBooks(book -> {
                book.setName("Jimmer, a new ORM");
                book.setEdition(1);
                book.setPrice(new BigDecimal("59.99"));
            });
        });
        executeAndExpectResult(
                getSqlClient()
                        .getEntities()
                        .saveCommand(store)
                        .setMode(SaveMode.INSERT_ONLY)
                        .setAssociatedModeAll(AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, WEBSITE, VERSION) values(?, ?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)");
                    });
                    ctx.totalRowCount(3);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 2);
                    ctx.entity(it -> {
                        it.original(
                                "{" +
                                        "--->\"name\":\"TURING\"," +
                                        "--->\"website\":\"https://www.turing.org\"," +
                                        "--->\"books\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"name\":\"SQL Optimization\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":59.99" +
                                        "--->--->},{" +
                                        "--->--->--->\"name\":\"Jimmer, a new ORM\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":59.99" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                        it.modified(
                                "{" +
                                        "--->\"id\":\"bdd0445b-c71f-46fd-80ed-b646d32b1351\"," +
                                        "--->\"name\":\"TURING\"," +
                                        "--->\"website\":\"https://www.turing.org\"," +
                                        "--->\"version\":0," +
                                        "--->\"books\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"8b16a8cf-cb8a-4781-87b8-652dc7a1d04f\"," +
                                        "--->--->--->\"name\":\"SQL Optimization\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":59.99," +
                                        "--->--->--->\"store\":{\"id\":\"bdd0445b-c71f-46fd-80ed-b646d32b1351\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"003b0ef9-f63f-480f-b752-348a23c00997\"," +
                                        "--->--->--->\"name\":\"Jimmer, a new ORM\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":59.99," +
                                        "--->--->--->\"store\":{\"id\":\"bdd0445b-c71f-46fd-80ed-b646d32b1351\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testAppendWildObjects() {
        Worker worker1 = WorkerDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.addIntoTasks(task -> {
                task.setName("Task-1");
            });
            draft.addIntoTasks(task -> {
                task.setName("Task-2");
            });
            draft.addIntoTasks(task -> {
                task.setName("Task-3");
            });
        });
        Worker worker2 = WorkerDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.addIntoTasks(task -> {
                task.setName("Task-4");
            });
            draft.addIntoTasks(task -> {
                task.setName("Task-5");
            });
        });

        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveEntitiesCommand(
                                Arrays.asList(worker1, worker2)
                        )
                        .setAssociatedMode(WorkerProps.TASKS, AssociatedSaveMode.APPEND),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TASK(NAME, OWNER_ID) values(?, ?)");
                        it.batchVariables(0, "Task-1", 1L);
                        it.batchVariables(1, "Task-2", 1L);
                        it.batchVariables(2, "Task-3", 1L);
                        it.batchVariables(3, "Task-4", 2L);
                        it.batchVariables(4, "Task-5", 2L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":1," +
                                        "--->\"tasks\":[" +
                                        "--->--->{\"id\":100,\"name\":\"Task-1\",\"owner\":{\"id\":1}}," +
                                        "--->--->{\"id\":101,\"name\":\"Task-2\",\"owner\":{\"id\":1}}," +
                                        "--->--->{\"id\":102,\"name\":\"Task-3\",\"owner\":{\"id\":1}}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":2," +
                                        "--->\"tasks\":[" +
                                        "--->--->{\"id\":103,\"name\":\"Task-4\",\"owner\":{\"id\":2}}," +
                                        "--->--->{\"id\":104,\"name\":\"Task-5\",\"owner\":{\"id\":2}}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testReplaceWildObjets() {
        Worker worker = WorkerDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.addIntoTasks(task -> {
                task.setName("Task-1");
            });
            draft.addIntoTasks(task -> {
                task.setName("Task-2");
            });
            draft.addIntoTasks(task -> {
                task.setName("Task-3");
            });
        });
        executeAndExpectResult(
                getSqlClient(it -> {
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveCommand(worker)
                        .setAssociatedMode(WorkerProps.TASKS, AssociatedSaveMode.VIOLENTLY_REPLACE),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from TASK where OWNER_ID = ?");
                        it.variables(2L);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TASK(NAME, OWNER_ID) values(?, ?)");
                        it.batchVariables(0, "Task-1", 2L);
                        it.batchVariables(1, "Task-2", 2L);
                        it.batchVariables(2, "Task-3", 2L);
                    });
                    ctx.entity(it -> {});
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Task.class), 4);
                }
        );
    }
}
