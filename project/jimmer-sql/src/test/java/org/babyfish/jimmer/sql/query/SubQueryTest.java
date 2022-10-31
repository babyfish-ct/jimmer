package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class SubQueryTest extends AbstractQueryTest {

    @Test
    public void testColumnInSubQuery() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.id().in(
                                    getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                        sq.where(author.firstName().eq("Alex"));
                                        return sq.select(author.books().id());
                                    })
                            )
                    );
                    return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                        Expression.tuple(book.name(), book.price()).in(
                                getLambdaClient().createSubQuery(q, BookTableEx.class, (sq, book2) ->
                                        sq
                                                .groupBy(book2.name())
                                                .select(
                                                        book2.name(),
                                                        book2.price().max()
                                                )
                                )
                        )
                    );
                    return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                        q.where(
                                getLambdaClient().createWildSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                    sq.where(
                                            book.eq(author.books()),
                                            author.firstName().eq("Alex")
                                    );
                                }).exists()
                        );
                        return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                sq.where(
                                        book.eq(author.books()),
                                        author.firstName().eq("Alex")
                                );
                                return sq.select(author);
                            }).exists()
                    );
                    return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.price().gt(
                                    getLambdaClient().createSubQuery(q, BookTableEx.class, (sq, book2) -> {
                                        return sq.select(book2.price().avg().coalesce(BigDecimal.ZERO));
                                    })
                            )
                    );
                    return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    TypedSubQuery<BigDecimal> subQuery =
                            getLambdaClient().createSubQuery(q, BookTableEx.class, (sq, book) -> {
                                sq.where(store.eq(book.store()));
                                return sq.select(
                                        book.price().avg().coalesce(BigDecimal.ZERO)
                                );
                            });
                    q.orderBy(subQuery.desc());
                    return q.select(store, subQuery);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.id().eq(
                                    getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                        sq.where(author.firstName().in(Arrays.asList("Alex", "Bill")));
                                        return sq.select(author.books().id());
                                    }).any()
                            )
                    );
                    return q.select(book);
                }),
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
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.id().eq(
                                    getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                        sq.where(author.firstName().in(Arrays.asList("Alex", "Bill")));
                                        return sq.select(author.books().id());
                                    }).all()
                            )
                    );
                    return q.select(book);
                }),
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
