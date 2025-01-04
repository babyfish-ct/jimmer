package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;
import static org.babyfish.jimmer.sql.common.Constants.oreillyId;

public class SQLiteSaveCommandTest extends AbstractMutationTest {

    @BeforeAll
    public static void beforeAll() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void insertOnlyTest() {
        UUID bookStoreId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).getEntities().saveCommand(Immutables.createBookStore(bookStore -> {
                            bookStore.setId(bookStoreId);
                            bookStore.setName("GitHub");
                            bookStore.setWebsite("https://udlbook.github.io/udlbook/");
                            bookStore.setVersion(1);
                            bookStore.addIntoBooks(book -> {
                                book.setId(bookId);
                                book.setName("Understanding Deep Learning");
                                book.setEdition(1);
                                book.setPrice(BigDecimal.ONE);
                                book.addIntoAuthors(author -> {
                                    author.setId(authorId);
                                    author.setFirstName("Prince");
                                    author.setLastName("Simon");
                                    author.setGender(Gender.MALE);
                                });
                            });
                        })
                ).setMode(SaveMode.INSERT_ONLY), ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, WEBSITE, VERSION) values(?, ?, ?, ?)");
                        it.variables(bookStoreId, "GitHub", "https://udlbook.github.io/udlbook/", 1);
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.STORE_ID from BOOK tb_1_ where tb_1_.ID = ?");
                        it.variables(bookId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?)");
                        it.variables(bookId, "Understanding Deep Learning", 1, BigDecimal.ONE, bookStoreId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?) " +
                                "on conflict(ID) do update set FIRST_NAME = excluded.FIRST_NAME, LAST_NAME = excluded.LAST_NAME, GENDER = excluded.GENDER");
                        it.variables(authorId, "Prince", "Simon", "M");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?)");
                        it.variables(bookId, authorId);
                    });
                    ctx.entity(it -> {
                        it.original(String.format("{\"id\":\"%s\",\"name\":\"GitHub\",\"website\":\"https://udlbook.github.io/udlbook/\",\"version\":1,\"books\":[{\"id\":\"%s\",\"name\":\"Understanding Deep Learning\",\"edition\":1,\"price\":1,\"authors\":[{\"id\":\"%s\",\"firstName\":\"Prince\",\"lastName\":\"Simon\",\"gender\":\"MALE\"}]}]}", bookStoreId, bookId, authorId));
                        it.modified(String.format("{\"id\":\"%s\",\"name\":\"GitHub\",\"website\":\"https://udlbook.github.io/udlbook/\",\"version\":1,\"books\":[{\"id\":\"%s\",\"name\":\"Understanding Deep Learning\",\"edition\":1,\"price\":1,\"store\":{\"id\":\"%s\"},\"authors\":[{\"id\":\"%s\",\"firstName\":\"Prince\",\"lastName\":\"Simon\",\"gender\":\"MALE\"}]}]}", bookStoreId, bookId, bookStoreId, authorId));
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(Author.class), 1);
                });
    }


    @Test
    public void testSaveLonely() {
        UUID bookId = UUID.randomUUID();
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).getEntities().saveCommand(Immutables.createBook(book -> {
                            book.setId(bookId);
                            book.setName("GraphQL in Action+");
                            book.setEdition(4);
                            book.setPrice(BigDecimal.valueOf(76));
                        })
                ), ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE) values(?, ?, ?, ?) on conflict(ID) " +
                                "do update set NAME = excluded.NAME, EDITION = excluded.EDITION, PRICE = excluded.PRICE");
                        it.variables(bookId, "GraphQL in Action+", 4, BigDecimal.valueOf(76));
                    });
                    ctx.entity(it -> {
                        it.original(String.format("{\"id\":\"%s\",\"name\":\"GraphQL in Action+\",\"edition\":4,\"price\":76}", bookId));
                        it.modified(String.format("{\"id\":\"%s\",\"name\":\"GraphQL in Action+\",\"edition\":4,\"price\":76}", bookId));
                    });
                    ctx.totalRowCount(1);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                });
    }

    @Test
    public void testShallowTree() {
        UUID bookId = UUID.randomUUID();
        UUID bookStoreId = UUID.randomUUID();
        UUID author1Id = UUID.randomUUID();
        UUID author2Id = UUID.randomUUID();
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).getEntities().saveCommand(Immutables.createBook(book -> {
                            book.setId(bookId);
                            book.setName("Computer Vision: Algorithms And Applications");
                            book.setEdition(3);
                            book.setPrice(BigDecimal.valueOf(52));
                            book.setStore(Immutables.createBookStore(store -> store.setId(bookStoreId).setName("Amazon")));
                            book.addIntoAuthors(author -> author.setId(author1Id));
                            book.addIntoAuthors(author -> author.setId(author2Id));
                        })
                ), ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.ID = ?");
                        it.variables(bookStoreId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(bookStoreId, "Amazon", 0);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?) " +
                                "on conflict(ID) do update set NAME = excluded.NAME, EDITION = excluded.EDITION, PRICE = excluded.PRICE, STORE_ID = excluded.STORE_ID");
                        it.variables(bookId, "Computer Vision: Algorithms And Applications", 3, BigDecimal.valueOf(52), bookStoreId);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID not in (?, ?)");
                        it.variables(bookId, author1Id, author2Id);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?) " +
                                        "on conflict(BOOK_ID, AUTHOR_ID) do nothing"
                        );
                        it.variables(bookId, author1Id);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values(?, ?) " +
                                        "on conflict(BOOK_ID, AUTHOR_ID) do nothing"
                        );
                        it.variables(bookId, author2Id);
                    });
                    ctx.entity(it -> {
                        it.original(String.format("{\"id\":\"%s\",\"name\":\"Computer Vision: Algorithms And Applications\",\"edition\":3,\"price\":52,\"store\":{\"id\":\"%s\",\"name\":\"Amazon\"},\"authors\":[{\"id\":\"%s\"},{\"id\":\"%s\"}]}", bookId, bookStoreId, author1Id, author2Id));
                        it.modified(String.format("{\"id\":\"%s\",\"name\":\"Computer Vision: Algorithms And Applications\",\"edition\":3,\"price\":52,\"store\":{\"id\":\"%s\",\"name\":\"Amazon\",\"version\":0},\"authors\":[{\"id\":\"%s\"},{\"id\":\"%s\"}]}", bookId, bookStoreId, author1Id, author2Id));
                    });
                    ctx.totalRowCount(4);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 2);
                });
    }

    @Test
    public void testDeepTree() {
        UUID bookId = UUID.randomUUID();
        UUID bookStoreId = UUID.randomUUID();
        UUID author1Id = UUID.randomUUID();
        UUID author2Id = UUID.randomUUID();
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).getEntities().saveCommand(Immutables.createBook(book -> {
                            book.setId(bookId);
                            book.setName("Machine Learning Design Patterns");
                            book.setEdition(3);
                            book.setPrice(BigDecimal.valueOf(36));
                            book.setStore(Immutables.createBookStore(store -> store.setId(bookStoreId).setName("Amazon")));
                            book.addIntoAuthors(author -> {
                                author.setId(author1Id);
                                author.setFirstName("Lakshmanan");
                                author.setLastName("Valliappa");
                                author.setGender(Gender.MALE);
                            });
                            book.addIntoAuthors(author -> {
                                author.setId(author2Id);
                                author.setFirstName("Munn");
                                author.setLastName("Michael");
                                author.setGender(Gender.MALE);
                            });
                        })
                ), ctx -> {
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_ where tb_1_.ID = ?");
                        it.variables(bookStoreId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)");
                        it.variables(bookStoreId, "Amazon", 0);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK(ID, NAME, EDITION, PRICE, STORE_ID) values(?, ?, ?, ?, ?) on conflict(ID) do update set NAME = excluded.NAME, EDITION = excluded.EDITION, PRICE = excluded.PRICE, STORE_ID = excluded.STORE_ID");
                        it.variables(bookId, "Machine Learning Design Patterns", 3, BigDecimal.valueOf(36), bookStoreId);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?) on conflict(ID) do update set FIRST_NAME = excluded.FIRST_NAME, LAST_NAME = excluded.LAST_NAME, GENDER = excluded.GENDER");
                        it.variables(author1Id, "Lakshmanan", "Valliappa", "M");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into AUTHOR(ID, FIRST_NAME, LAST_NAME, GENDER) values(?, ?, ?, ?) on conflict(ID) do update set FIRST_NAME = excluded.FIRST_NAME, LAST_NAME = excluded.LAST_NAME, GENDER = excluded.GENDER");
                        it.variables(author2Id, "Munn", "Michael", "M");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID = ? and AUTHOR_ID not in (?, ?)");
                        it.variables(bookId, author1Id, author2Id);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?) on conflict(BOOK_ID, AUTHOR_ID) do nothing");
                        it.variables(bookId, author1Id);
                    });
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?) on conflict(BOOK_ID, AUTHOR_ID) do nothing");
                        it.variables(bookId, author2Id);
                    });
                    ctx.entity(it -> {
                        it.original(String.format("{\"id\":\"%s\",\"name\":\"Machine Learning Design Patterns\",\"edition\":3,\"price\":36,\"store\":{\"id\":\"%s\",\"name\":\"Amazon\"},\"authors\":[{\"id\":\"%s\",\"firstName\":\"Lakshmanan\",\"lastName\":\"Valliappa\",\"gender\":\"MALE\"},{\"id\":\"%s\",\"firstName\":\"Munn\",\"lastName\":\"Michael\",\"gender\":\"MALE\"}]}", bookId, bookStoreId, author1Id, author2Id));
                        it.modified(String.format("{\"id\":\"%s\",\"name\":\"Machine Learning Design Patterns\",\"edition\":3,\"price\":36,\"store\":{\"id\":\"%s\",\"name\":\"Amazon\",\"version\":0},\"authors\":[{\"id\":\"%s\",\"firstName\":\"Lakshmanan\",\"lastName\":\"Valliappa\",\"gender\":\"MALE\"},{\"id\":\"%s\",\"firstName\":\"Munn\",\"lastName\":\"Michael\",\"gender\":\"MALE\"}]}", bookId, bookStoreId, author1Id, author2Id));
                    });
                    ctx.totalRowCount(6);
                    ctx.rowCount(AffectedTable.of(Book.class), 1);
                    ctx.rowCount(AffectedTable.of(BookStore.class), 1);
                    ctx.rowCount(AffectedTable.of(Author.class), 2);
                    ctx.rowCount(AffectedTable.of(BookProps.AUTHORS), 2);
                    ctx.rowCount(AffectedTable.of(AuthorProps.BOOKS), 2);
                });
    }

    @Test
    public void testOptimisticLock() {
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).getEntities().saveCommand(Immutables.createBook(book -> {
                    book.setId(learningGraphQLId1);
                    book.setName("Learning GraphQL");
                    book.setPrice(BigDecimal.valueOf(49));
                })).setMode(SaveMode.UPDATE_ONLY).setOptimisticLock(BookTable.class, (table, it) -> {
                    return table.edition().eq(1);
                }), ctx -> {
                    ctx.statement(sql -> {
                        sql.sql("update BOOK set NAME = ?, PRICE = ? where ID = ? and EDITION = ?");
                        sql.variables("Learning GraphQL", BigDecimal.valueOf(49), learningGraphQLId1, 1);
                    });
                    ctx.entity(entity -> {
                        entity.original(String.format("{\"id\":\"%s\",\"name\":\"Learning GraphQL\",\"price\":49}", learningGraphQLId1));
                        entity.modified(String.format("{\"id\":\"%s\",\"name\":\"Learning GraphQL\",\"price\":49}", learningGraphQLId1));
                    });
                }
        );
    }

    @Test
    public void testSingleForIssue862() {
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SQLiteDialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setTargetTransferable(true);
                }).saveCommand(
                        Immutables.createDepartment(draft -> {
                            draft.setName("Develop");
                            draft.addIntoEmployees(emp -> emp.setName("Tom").setGender(Gender.MALE));
                            draft.addIntoEmployees(emp -> emp.setName("Nancy").setGender(Gender.FEMALE));
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.NO_ID_UPSERT_NOT_SUPPORTED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = ? " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Develop", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into DEPARTMENT(NAME, DELETED_MILLIS) " +
                                        "values(?, ?)"
                        );
                        it.variables("Develop", 0L);
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.NO_ID_UPSERT_NOT_SUPPORTED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.NAME in (?, ?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Tom", "Nancy", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("Tom", "M", 0L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("Nancy", "F", 0L, 2L);
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"2\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"3\"," +
                                        "--->--->--->\"name\":\"Tom\"," +
                                        "--->--->--->\"gender\":\"MALE\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"2\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"4\"," +
                                        "--->--->--->\"name\":\"Nancy\"," +
                                        "--->--->--->\"gender\":\"FEMALE\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"2\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }

    @Test
    public void testBatchForIssue862() {
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SQLiteDialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setTargetTransferable(true);
                }).saveEntitiesCommand(
                        Arrays.asList(
                                Immutables.createDepartment(draft -> {
                                    draft.setName("Develop");
                                    draft.addIntoEmployees(emp -> emp.setName("Tom").setGender(Gender.MALE));
                                    draft.addIntoEmployees(emp -> emp.setName("Nancy").setGender(Gender.FEMALE));
                                }),
                                Immutables.createDepartment(draft -> {
                                    draft.setName("Market");
                                    draft.addIntoEmployees(emp -> emp.setName("Alex").setGender(Gender.MALE));
                                    draft.addIntoEmployees(emp -> emp.setName("Jessica").setGender(Gender.FEMALE));
                                })
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.NO_ID_UPSERT_NOT_SUPPORTED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME in (?, ?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Develop", "Market", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?)"
                        );
                        it.variables("Develop", 0L);
                    });
                    ctx.statement(it -> {
                        it.queryReason(QueryReason.NO_ID_UPSERT_NOT_SUPPORTED);
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME " +
                                        "from EMPLOYEE tb_1_ " +
                                        "where tb_1_.NAME in (?, ?, ?, ?) " +
                                        "and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Tom", "Nancy", "Alex", "Jessica", 0L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("Tom", "M", 0L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("Nancy", "F", 0L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into EMPLOYEE(NAME, GENDER, DELETED_MILLIS, DEPARTMENT_ID) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("Alex", "M", 0L, 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set GENDER = ?, " +
                                        "DEPARTMENT_ID = ? " +
                                        "where ID = ?"
                        );
                        it.variables("F", 1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set DELETED_MILLIS = ? " +
                                        "where DEPARTMENT_ID in (?, ?) " +
                                        "and (DEPARTMENT_ID, ID) not in ((?, ?), (?, ?), (?, ?), (?, ?)) " +
                                        "and DELETED_MILLIS = ?"
                        );
                        it.variables(
                                UNKNOWN_VARIABLE,
                                2L, 1L,
                                2L, 3L, 2L, 4L, 1L, 5L, 1L, 2L,
                                0L
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"2\"," +
                                        "--->\"name\":\"Develop\"," +
                                        "--->\"deletedMillis\":0," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"3\"," +
                                        "--->--->--->\"name\":\"Tom\"," +
                                        "--->--->--->\"gender\":\"MALE\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"2\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"4\"," +
                                        "--->--->--->\"name\":\"Nancy\"," +
                                        "--->--->--->\"gender\":\"FEMALE\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"2\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"name\":\"Market\"," +
                                        "--->\"employees\":[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"5\"," +
                                        "--->--->--->\"name\":\"Alex\"," +
                                        "--->--->--->\"gender\":\"MALE\"," +
                                        "--->--->--->\"deletedMillis\":0," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->},{" +
                                        "--->--->--->\"id\":\"2\"," +
                                        "--->--->--->\"name\":\"Jessica\"," +
                                        "--->--->--->\"gender\":\"FEMALE\"," +
                                        "--->--->--->\"department\":{\"id\":\"1\"}" +
                                        "--->--->}" +
                                        "--->]" +
                                        "}"
                        );
                    });
                }
        );
    }
}
