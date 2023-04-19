package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class PagingTest extends AbstractQueryTest {

    @Test
    public void testCountQuerySkipUnnecessaryJoinOfIgnoredOrderByClause() {

        ConfigurableRootQuery<BookTable, Book> query = getLambdaClient().createQuery(BookTable.class, (q, book) -> {
            q.where(book.price().between(new BigDecimal(20), new BigDecimal(30)));
            q.orderBy(book.store(JoinType.LEFT).name());
            q.orderBy(book.name());
            return q.select(book);
        });

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book) -> q.select(book.count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(tb_1_.ID) " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.limit(10, 20),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "left join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_2_.NAME asc, tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20);
                }
        );
    }

    @Test
    public void testCountQueryKeepNecessaryJoinOfIgnoredOrderByClause() {

        ConfigurableRootQuery<BookTable, Book> query = getLambdaClient().createQuery(BookTable.class, (q, book) -> {
            q.where(book.price().between(new BigDecimal(20), new BigDecimal(30)));
            q.orderBy(book.store().name());
            q.orderBy(book.name());
            return q.select(book);
        });

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book) -> q.select(book.count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(tb_1_.ID) " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.limit(10, 20),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_2_.NAME asc, tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20);
                }
        );
    }

    @Test
    public void testCountQuerySkipNecessaryJoinOfIgnoredSelectClause() {

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query =
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.price().between(new BigDecimal(20), new BigDecimal(30)));
                    q.orderBy(book.name());
                    return q.select(book, book.store(JoinType.LEFT));
                });

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book) -> q.select(book.count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(tb_1_.ID) " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.limit(10, 20),
                ctx -> {
                    ctx.sql(
                            "select " +
                                        "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                        "tb_1_.STORE_ID, tb_2_.NAME, tb_2_.WEBSITE, tb_2_.VERSION " +
                                    "from BOOK as tb_1_ " +
                                    "left join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20);
                }
        );
    }

    @Test
    public void testCountQueryKeepNecessaryJoinOfIgnoredSelectClause() {

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query =
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.price().between(new BigDecimal(20), new BigDecimal(30)));
                    q.orderBy(book.name());
                    return q.select(book, book.store());
                });

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book) -> q.select(book.count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(tb_1_.ID) " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.limit(10, 20),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                    "tb_1_.STORE_ID, tb_2_.NAME, tb_2_.WEBSITE, tb_2_.VERSION " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20);
                }
        );
    }

    @Test
    public void testReselectTwice() {

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query =
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.price().between(new BigDecimal(20), new BigDecimal(30)));
                    q.orderBy(book.name());
                    return q.select(book, book.store());
                });

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query
                    .reselect((q, book) -> q.select(book.count()))
                    .reselect((q, book) -> q.select(book.count()));
        });
    }

    @Test
    public void testReselectBaseOnGroupBy() {

        ConfigurableRootQuery<BookTable, UUID> query =
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.groupBy(book.id());
                    return q.select(book.id());
                });

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query.reselect((q, book) -> q.select(book.count()));
        });
    }

    @Test
    public void testReselectBaseOnAggregation() {

        ConfigurableRootQuery<BookTable, Long> query =
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(book.count());
                });

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query.reselect((q, book) -> q.select(book.count()));
        });
    }

    @Test
    public void testMySqlDialect() {
        executeAndExpect(
                getLambdaClient(
                        it -> it.setDialect(new MySqlDialect())
                ).createQuery(BookTable.class, (q, book) -> {
                    q.orderBy(book.name());
                    return q.select(book.name());
                }).distinct().limit(2, 1),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ?, ?"
                    );
                    ctx.variables(1, 2);
                    ctx.rows(Arrays.asList("GraphQL in Action", "Learning GraphQL"));
                }
        );
    }

    @Test
    public void testSqlServerDialect() {
        executeAndExpect(
                getLambdaClient(
                        it -> it.setDialect(new SqlServerDialect())
                ).createQuery(BookTable.class, (q, book) -> {
                    q.orderBy(book.name());
                    return q.select(book.name());
                }).distinct().limit(2, 1),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "order by tb_1_.NAME asc " +
                                    "offset ? rows fetch next ? rows only"
                    );
                    ctx.variables(1, 2);
                    ctx.rows(Arrays.asList("GraphQL in Action", "Learning GraphQL"));
                }
        );
    }

    @Test
    public void testOracleDialect() {
        executeAndExpect(
                getLambdaClient(
                        it -> it.setDialect(new OracleDialect())
                ).createQuery(BookTable.class, (q, book) -> {
                    q.orderBy(book.name());
                    return q.select(book.name());
                }).distinct().limit(2, 1),
                ctx -> {
                    ctx.sql(
                            "select * from (" +
                                        "select core__.*, rownum rn__ " +
                                        "from (" +
                                            "select distinct tb_1_.NAME " +
                                            "from BOOK as tb_1_ " +
                                            "order by tb_1_.NAME asc" +
                                        ") core__ where rownum <= ?" +
                                    ") limited__ where rn__ > ?"
                    );
                    ctx.variables(3, 1);
                    ctx.rows(Arrays.asList("GraphQL in Action", "Learning GraphQL"));
                }
        );
    }

    @Test
    public void testOracleDialectWithOnlyLimit() {
        executeAndExpect(
                getLambdaClient(
                        it -> it.setDialect(new OracleDialect())
                ).createQuery(BookTable.class, (q, book) -> {
                    q.orderBy(book.name());
                    return q.select(book.name());
                }).distinct().limit(2, 0),
                ctx -> {
                    ctx.sql(
                            "select core__.* from (" +
                                    "select distinct tb_1_.NAME " +
                                    "from BOOK as tb_1_ " +
                                    "order by tb_1_.NAME asc" +
                                    ") core__ where rownum <= ?"
                    );
                    ctx.variables(2);
                    ctx.rows(Arrays.asList("Effective TypeScript", "GraphQL in Action"));
                }
        );
    }

    @Test
    public void testMinOffsetBySelf() {

        executeAndExpect(
                getLambdaClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(BookTable.class, (q, book) -> {
                            return q
                                    .where(book.store().name().eq("O'REILLY"))
                                    .orderBy(book.name().asc(), book.edition().desc())
                                    .select(book)
                                    .limit(3, 3);
                        }),
                ctx -> {
                    ctx.sql(
                            "select ID, NAME, EDITION, PRICE, STORE_ID " +
                                    "from BOOK " +
                                    "where ID in(" +
                                    "--->select tb_1_.ID " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->inner join BOOK_STORE as tb_2_ " +
                                    "--->--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "--->where tb_2_.NAME = ? " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? offset ?" +
                                    ")"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMinOffsetBySelfWithAssociation() {

        executeAndExpect(
                getLambdaClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(BookTable.class, (q, book) -> {
                            return q.where(book.store().name().eq("O'REILLY"))
                                    .orderBy(book.name().asc(), book.edition().desc())
                                    .select(
                                            book.fetch(
                                                    BookFetcher.$
                                                            .allScalarFields()
                                                            .price(false)
                                                            .authors(
                                                                    AuthorFetcher.$
                                                                            .allScalarFields()
                                                            )
                                            )
                                    )
                                    .limit(3, 3);
                        }),
                ctx -> {
                    ctx.sql(
                            "select ID, NAME, EDITION " +
                                    "from BOOK " +
                                    "where ID in(" +
                                    "--->select tb_1_.ID " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->inner join BOOK_STORE as tb_2_ " +
                                    "--->--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "--->where tb_2_.NAME = ? " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? " +
                                    "--->offset ?" +
                                    ")"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMinOffsetBySelfWithIdOnly() {

        executeAndExpect(
                getLambdaClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(BookTable.class, (q, book) -> {
                            return q.where(book.store().name().eq("O'REILLY"))
                                    .orderBy(book.name().asc(), book.edition().desc())
                                    .select(
                                            book.fetch(BookFetcher.$)
                                    )
                                    .limit(3, 3);
                        }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ " +
                                    "--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_2_.NAME = ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}," +
                                    "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                    "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMinOffsetByReferenceJoin() {

        executeAndExpect(
                getLambdaClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(BookTable.class, (q, book) -> {
                            return q.orderBy(book.name().asc(), book.edition().desc())
                                    .select(book.store())
                                    .limit(2, 2);
                        }),
                ctx -> {
                    ctx.sql(
                            "select ID, NAME, WEBSITE, VERSION from BOOK_STORE " +
                                    "where ID in(" +
                                    "--->select tb_1_.STORE_ID " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? " +
                                    "--->offset ?" +
                                    ")"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
