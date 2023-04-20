package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.query.ConfigurableRootQuery;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

public class FluentPagingTest extends AbstractQueryTest {

    @Test
    public void testCountQuerySkipUnnecessaryJoinOfIgnoredOrderByClause() {

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Book> query = getSqlClient()
                .createQuery(book)
                .where(book.price().between(new BigDecimal(20), new BigDecimal(30)))
                .orderBy(book.store(JoinType.LEFT).name())
                .orderBy(book.name())
                .select(book);

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book2) -> q.select(book2.count()))
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

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Book> query = getSqlClient()
                .createQuery(book)
                .where(book.price().between(new BigDecimal(20), new BigDecimal(30)))
                .orderBy(book.store().name())
                .orderBy(book.name())
                .select(book);

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book2) -> q.select(book2.count()))
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

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query = getSqlClient()
                .createQuery(book)
                .where(book.price().between(new BigDecimal(20), new BigDecimal(30)))
                .orderBy(book.name())
                .select(book, book.store(JoinType.LEFT));

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book2) -> q.select(book2.count()))
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

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query = getSqlClient()
                .createQuery(book)
                .where(book.price().between(new BigDecimal(20), new BigDecimal(30)))
                .orderBy(book.name())
                .select(book, book.store());

        ConfigurableRootQuery<BookTable, Long> countQuery = query
                .reselect((q, book2) -> q.select(book2.count()))
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

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Tuple2<Book, BookStore>> query =
                getSqlClient()
                        .createQuery(book)
                        .where(book.price().between(new BigDecimal(20), new BigDecimal(30)))
                        .orderBy(book.name())
                        .select(book, book.store());

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query
                    .reselect((q, book2) -> q.select(book2.count()))
                    .reselect((q, book2) -> q.select(book2.count()));
        });
    }

    @Test
    public void testReselectBaseOnGroupBy() {

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, UUID> query =
                getSqlClient().createQuery(book)
                        .groupBy(book.id())
                        .select(book.id());

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query.reselect((q, book2) -> q.select(book2.count()));
        });
    }

    @Test
    public void testReselectBaseOnAggregation() {

        BookTable book = BookTable.$;

        ConfigurableRootQuery<BookTable, Long> query =
                getSqlClient()
                        .createQuery(book)
                        .select(book.count());

        Assertions.assertThrows(IllegalStateException.class, () -> {
            query.reselect((q, book2) -> q.select(book2.count()));
        });
    }

    @Test
    public void testMySqlDialect() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(
                        it -> it.setDialect(new MySqlDialect())
                ).createQuery(book)
                        .orderBy(book.name())
                        .select(book.name())
                        .distinct()
                        .limit(2, 1),
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

//    @Test
//    public void testSqlServerDialect() {
//
//        BookTable book = BookTable.$;
//
//        executeAndExpect(
//                getSqlClient(
//                        it -> it.setDialect(new SqlServerDialect())
//                ).createQuery(book)
//                        .orderBy(book.name())
//                        .select(book.name())
//                        .distinct()
//                        .limit(2, 1),
//                ctx -> {
//                    ctx.sql(
//                            "select distinct tb_1_.NAME " +
//                                    "from BOOK as tb_1_ " +
//                                    "order by tb_1_.NAME asc " +
//                                    "offset ? rows fetch next ? rows only"
//                    );
//                    ctx.variables(1, 2);
//                    ctx.rows(Arrays.asList("GraphQL in Action", "Learning GraphQL"));
//                }
//        );
//    }

    @Test
    public void testOracleDialect() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(
                        it -> it.setDialect(new OracleDialect())
                ).createQuery(book)
                        .orderBy(book.name())
                        .select(book.name())
                        .distinct()
                        .limit(2, 1),
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

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(
                        it -> it.setDialect(new OracleDialect())
                ).createQuery(book)
                        .orderBy(book.name())
                        .select(book.name())
                        .distinct()
                        .limit(2, 0),
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
