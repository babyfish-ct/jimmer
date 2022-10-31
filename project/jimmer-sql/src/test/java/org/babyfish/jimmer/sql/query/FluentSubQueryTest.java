package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class FluentSubQueryTest extends AbstractQueryTest {

    @Test
    public void testColumnInSubQuery() {
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                book.id().in(
                                        getSqlClient().createSubQuery(author)
                                                .where(author.firstName().eq("Alex"))
                                                .select(author.books().id())
                                )
                        )
                        .select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID in (" +
                                        "select tb_3_.BOOK_ID " +
                                        "from AUTHOR as tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_2_.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testTwoColumnsInSubQuery() {
        BookTable book = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                Expression.tuple(book.name(), book.price()).in(
                                        getSqlClient().createSubQuery(book)
                                                .groupBy(book.name())
                                                .select(
                                                        book.name(),
                                                        book.price().max()
                                                )
                                )
                        )
                        .select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where (tb_1_.NAME, tb_1_.PRICE) in (" +
                                        "select tb_2_.NAME, max(tb_2_.PRICE) " +
                                        "from BOOK as tb_2_ " +
                                        "group by tb_2_.NAME" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testExists() {
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                getSqlClient()
                                        .createSubQuery(author)
                                        .where(
                                                book.eq(author.books()),
                                                author.firstName().eq("Alex")
                                        )
                                        .exists()
                        )
                        .select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ where exists (" +
                                        "select 1 " +
                                        "from AUTHOR as tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_1_.ID = tb_3_.BOOK_ID " +
                                        "and tb_2_.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testExistsWithTypedQuery() {

        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                getSqlClient()
                                        .createSubQuery(author)
                                        .where(
                                            book.eq(author.books()),
                                            author.firstName().eq("Alex")
                                        )
                                        .select(author)
                                        .exists()
                        )
                        .select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ where exists (" +
                                    "select 1 " +
                                    "from AUTHOR as tb_2_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "where tb_1_.ID = tb_3_.BOOK_ID " +
                                    "and tb_2_.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testSubQueryAsSimpleExpression() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(
                                book.price().gt(
                                    getSqlClient()
                                            .createSubQuery(book)
                                            .select(book.price().avg().coalesce(BigDecimal.ZERO))
                                )
                        )
                        .select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ where tb_1_.PRICE > (" +
                                        "select coalesce(avg(tb_2_.PRICE), ?) from BOOK as tb_2_" +
                                    ")"
                    );
                    ctx.variables(BigDecimal.ZERO);
                }
        );
    }

    @Test
    public void testSubQueryAsSelectionOrderByClause() {

        BookStoreTable store = BookStoreTable.$;
        BookTable book = BookTable.$;

        TypedSubQuery<BigDecimal> subQuery =
                getSqlClient().createSubQuery(book)
                        .where(store.eq(book.store()))
                        .select(
                            book.price().avg().coalesce(BigDecimal.ZERO)
                        );
        executeAndExpect(
                getSqlClient().createQuery(store)
                    .orderBy(subQuery.desc())
                    .select(store, subQuery),
                ctx -> {
                    ctx.sql(
                            "select " +
                                        "tb_1_.ID, " +
                                        "tb_1_.NAME, " +
                                        "tb_1_.WEBSITE, " +
                                        "tb_1_.VERSION, " +
                                        "(" +
                                            "select coalesce(avg(tb_2_.PRICE), ?) " +
                                            "from BOOK as tb_2_ " +
                                            "where tb_1_.ID = tb_2_.STORE_ID" +
                                        ") " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "order by (" +
                                        "select coalesce(avg(tb_2_.PRICE), ?) " +
                                        "from BOOK as tb_2_ " +
                                        "where tb_1_.ID = tb_2_.STORE_ID" +
                                    ") desc"
                    );
                    ctx.variables(BigDecimal.ZERO, BigDecimal.ZERO);
                }
        );
    }

    @Test
    public void testSubQueryWithAny() {

        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpect(
                getSqlClient().createQuery(book)
                        .where(
                                book.id().eq(
                                        getSqlClient()
                                                .createSubQuery(author)
                                                .where(author.firstName().in(Arrays.asList("Alex", "Bill")))
                                                .select(author.books().id())
                                                .any()
                                )
                        ).select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID = any(" +
                                        "select tb_3_.BOOK_ID " +
                                        "from AUTHOR as tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_2_.FIRST_NAME in (?, ?)" +
                                    ")"
                    );
                    ctx.variables("Alex", "Bill");
                }
        );
    }

    @Test
    public void testSubQueryWithAll() {

        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpect(
                getSqlClient().createQuery(book)
                        .where(
                                book.id().eq(
                                        getSqlClient()
                                                .createSubQuery(author)
                                                .where(author.firstName().in(Arrays.asList("Alex", "Bill")))
                                                .select(author.books().id())
                                                .all()
                                )
                        ).select(book),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID = all(" +
                                        "select tb_3_.BOOK_ID " +
                                        "from AUTHOR as tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_2_.FIRST_NAME in (?, ?)" +
                                    ")"
                    );
                    ctx.variables("Alex", "Bill");
                }
        );
    }
}
