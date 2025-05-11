package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.query.PageFactory;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
                .reselect((q, book) -> q.select(Expression.constant(1).count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.offset(20).limit(10),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_2_.NAME asc, tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20L);
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
                .reselect((q, book) -> q.select(Expression.constant(1).count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.offset(20).limit(10),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_2_.NAME asc, tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20L);
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
                .reselect((q, book) -> q.select(Expression.constant(1).count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.offset(20).limit(10),
                ctx -> {
                    ctx.sql(
                            "select " +
                                        "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                        "tb_1_.STORE_ID, tb_2_.NAME, tb_2_.WEBSITE, tb_2_.VERSION " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20L);
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
                .reselect((q, book) -> q.select(Expression.constant(1).count()))
                .withoutSortingAndPaging();

        executeAndExpect(
                countQuery,
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30));
                }
        );

        executeAndExpect(
                query.offset(20).limit(10),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                    "tb_1_.STORE_ID, tb_2_.NAME, tb_2_.WEBSITE, tb_2_.VERSION " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.PRICE between ? and ? " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), 10, 20L);
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
                }).distinct().offset(1).limit(2),
                ctx -> {
                    ctx.sql(
                            "select distinct tb_1_.NAME " +
                                    "from BOOK tb_1_ " +
                                    "order by tb_1_.NAME asc " +
                                    "limit ?, ?"
                    );
                    ctx.variables(1L, 2);
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
                }).distinct().offset(1).limit(2),
                ctx -> {
                    ctx.sql(
                            "select * from (" +
                                        "select core__.*, rownum rn__ " +
                                        "from (" +
                                            "select distinct tb_1_.NAME " +
                                            "from BOOK tb_1_ " +
                                            "order by tb_1_.NAME asc" +
                                        ") core__ where rownum <= ?" +
                                    ") limited__ where rn__ > ?"
                    );
                    ctx.variables(3L, 1L);
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
                }).distinct().offset(0).limit(2),
                ctx -> {
                    ctx.sql(
                            "select core__.* from (" +
                                    "select distinct tb_1_.NAME " +
                                    "from BOOK tb_1_ " +
                                    "order by tb_1_.NAME asc" +
                                    ") core__ where rownum <= ?"
                    );
                    ctx.variables(2L);
                    ctx.rows(Arrays.asList("Effective TypeScript", "GraphQL in Action"));
                }
        );
    }

    @Test
    public void testDoubleLimit() {
        AuthorTable author = AuthorTable.$;
        BookTableEx book = BookTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(author)
                        .where(
                                author.id().in(
                                        getSqlClient()
                                                .createSubQuery(book)
                                                .orderBy(book.price().desc())
                                                .select(book.authors().id())
                                                .limit(4)
                                )
                        )
                        .orderBy(author.firstName(), author.lastName())
                        .select(author)
                        .limit(4),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "where tb_1_.ID in (" +
                                    "--->select tb_3_.AUTHOR_ID " +
                                    "--->from BOOK tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ " +
                                    "--->--->on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "--->order by tb_2_.PRICE desc " +
                                    "--->limit ?" +
                                    ") " +
                                    "order by tb_1_.FIRST_NAME asc, tb_1_.LAST_NAME asc " +
                                    "limit ?"
                    );
                }
        );
    }

    @Test
    public void testPagingQueries() {

        PageFactory<Book, Page<Book>> pageFactory =
                (entities, totalCount, source) -> new Page<>(entities, totalCount);
        BookTable table = BookTable.$;

        anyAndExpect(
                con -> getSqlClient()
                        .createQuery(table)
                        .where(table.name().like("GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(table)
                        .fetchPage(-1, 3, con, pageFactory),
                ctx -> {
                    ctx.rows(it -> {
                        expect(
                                "Page{entities=[], totalRowCount=0}",
                                it.get(0)
                        );
                    });
                }
        );

        anyAndExpect(
                con -> getSqlClient()
                        .createQuery(table)
                        .where(table.name().like("GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(table)
                        .fetchPage(0, 3, con, pageFactory),
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "limit ?"
                    );
                    ctx.rows(it -> {
                        expect(
                                "Page{" +
                                        "--->entities=[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->--->\"edition\":3," +
                                        "--->--->--->\"price\":80.00," +
                                        "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "--->--->}, {" +
                                        "--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->--->\"edition\":2," +
                                        "--->--->--->\"price\":81.00," +
                                        "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "--->--->}, {" +
                                        "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":80.00," +
                                        "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "--->--->}" +
                                        "--->], " +
                                        "--->totalRowCount=6" +
                                        "}",
                                it.get(0)
                        );
                    });
                }
        );

        anyAndExpect(
                con -> getSqlClient(it -> it.setReverseSortOptimizationEnabled(true))
                        .createQuery(table)
                        .where(table.name().like("GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(table)
                        .fetchPage(1, 3, con, pageFactory),
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ? " +
                                    "/* reverse sorting optimization */ " +
                                    "order by tb_1_.NAME desc, tb_1_.EDITION asc " +
                                    "limit ?"
                    );
                    ctx.rows(it -> {
                        expect(
                                "Page{" +
                                        "--->entities=[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                        "--->--->--->\"name\":\"Learning GraphQL\"," +
                                        "--->--->--->\"edition\":3," +
                                        "--->--->--->\"price\":51.00," +
                                        "--->--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                        "--->--->}, {" +
                                        "--->--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                        "--->--->--->\"name\":\"Learning GraphQL\"," +
                                        "--->--->--->\"edition\":2," +
                                        "--->--->--->\"price\":55.00," +
                                        "--->--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                        "--->--->}, {" +
                                        "--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                        "--->--->--->\"name\":\"Learning GraphQL\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":50.00," +
                                        "--->--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                        "--->--->}" +
                                        "--->], " +
                                        "--->totalRowCount=6" +
                                        "}",
                                it.get(0)
                        );
                    });
                }
        );

        anyAndExpect(
                con -> getSqlClient()
                        .createQuery(table)
                        .where(table.name().like("GraphQL"))
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(table)
                        .fetchPage(2, 3, con, pageFactory),
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME like ?"
                    );
                    ctx.rows(it -> {
                        expect(
                                "Page{entities=[], totalRowCount=6}",
                                it.get(0)
                        );
                    });
                }
        );

        anyAndExpect(
                con -> getSqlClient(it -> it.setReverseSortOptimizationEnabled(true))
                        .createQuery(table)
                        .where(table.store().id().eq(Constants.manningId))
                        .orderBy(table.edition().desc())
                        .select(table).fetchPage(1, 2, con, pageFactory),
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ where tb_1_.STORE_ID = ? " +
                                    "/* reverse sorting optimization */ " +
                                    "order by tb_1_.EDITION asc limit ?"
                    ).variables(Constants.manningId, 1);
                    ctx.rows(it -> {
                        expect(
                                "Page{" +
                                        "--->entities=[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":80.00," +
                                        "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "--->--->}" +
                                        "--->], " +
                                        "--->totalRowCount=3" +
                                        "}",
                                it.get(0)
                        );
                    });
                }
        );

        anyAndExpect(
                con -> getSqlClient()
                        .createQuery(table)
                        .where(table.store().id().eq(Constants.manningId))
                        .orderBy(table.edition().desc())
                        .select(table).fetchPage(1, 2, con, pageFactory),
                ctx -> {
                    ctx.sql(
                            "select count(1) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ where tb_1_.STORE_ID = ? " +
                                    "order by tb_1_.EDITION desc limit ? offset ?"
                    ).variables(Constants.manningId, 2, 2L);
                    ctx.rows(it -> {
                        expect(
                                "Page{" +
                                        "--->entities=[" +
                                        "--->--->{" +
                                        "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                                        "--->--->--->\"edition\":1," +
                                        "--->--->--->\"price\":80.00," +
                                        "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "--->--->}" +
                                        "--->], " +
                                        "--->totalRowCount=3" +
                                        "}",
                                it.get(0)
                        );
                    });
                }
        );
    }

    private static class Page<E> {

        final List<E> entities;

        final long totalRowCount;

        private Page(List<E> entities, long totalRowCount) {
            this.entities = entities;
            this.totalRowCount = totalRowCount;
        }

        @Override
        public String toString() {
            return "Page{" +
                    "entities=" + entities +
                    ", totalRowCount=" + totalRowCount +
                    '}';
        }
    }
}
