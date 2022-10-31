package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookTable;
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
}
