package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.OrderMode;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class SubQueryTest extends AbstractQueryTest  {

    @Test
    public void testColumnInSubQuery() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.id().in(
                                    AuthorTable.createSubQuery(q, (sq, author) -> {
                                        sq.where(author.firstName().eq("Alex"));
                                        return sq.select(author.books().id());
                                    })
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where tb_1.ID in (" +
                                        "select tb_3.BOOK_ID " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_2.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testTwoColumnsInSubQuery() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                        Expression.tuple(book.name(), book.price()).in(
                                BookTable.createSubQuery(q, (sq, book2) ->
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where (tb_1.NAME, tb_1.PRICE) in (" +
                                        "select tb_2.NAME, max(tb_2.PRICE) " +
                                        "from BOOK as tb_2 " +
                                        "group by tb_2.NAME" +
                                    ")"
                    );
                }
        );
    }

    @Test
    public void testExists() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                        q.where(
                                AuthorTable.createWildSubQuery(q, (sq, author) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 where exists (" +
                                        "select 1 " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_1.ID = tb_3.BOOK_ID " +
                                        "and tb_2.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testExistsWithTypedQuery() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            AuthorTable.createSubQuery(q, (sq, author) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 where exists (" +
                                    "select 1 " +
                                    "from AUTHOR as tb_2 " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                    "where tb_1.ID = tb_3.BOOK_ID " +
                                    "and tb_2.FIRST_NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Alex");
                }
        );
    }

    @Test
    public void testSubQueryAsSimpleExpression() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.price().gt(
                                    BookTable.createSubQuery(q, (sq, book2) -> {
                                        return sq.select(book.price().avg().coalesce(BigDecimal.ZERO));
                                    })
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 where tb_1.PRICE > (" +
                                        "select coalesce(avg(tb_1.PRICE), ?) from BOOK as tb_2" +
                                    ")"
                    );
                    ctx.variables(BigDecimal.ZERO);
                }
        );
    }

    @Test
    public void testSubQueryAsSelectionOrderByClause() {
        executeAndExpect(
                BookStoreTable.createQuery(getSqlClient(), (q, store) -> {
                    TypedSubQuery<BigDecimal> subQuery =
                            BookTable.createSubQuery(q, (sq, book) -> {
                                sq.where(store.eq(book.store()));
                                return sq.select(
                                        book.price().avg().coalesce(BigDecimal.ZERO)
                                );
                            });
                    q.orderBy(subQuery, OrderMode.DESC);
                    return q.select(store, subQuery);
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                        "tb_1.ID, " +
                                        "tb_1.NAME, " +
                                        "tb_1.WEBSITE, " +
                                        "(" +
                                            "select coalesce(avg(tb_2.PRICE), ?) " +
                                            "from BOOK as tb_2 " +
                                            "where tb_1.ID = tb_2.STORE_ID" +
                                        ") " +
                                    "from BOOK_STORE as tb_1 " +
                                    "order by (" +
                                        "select coalesce(avg(tb_2.PRICE), ?) " +
                                        "from BOOK as tb_2 " +
                                        "where tb_1.ID = tb_2.STORE_ID" +
                                    ") desc"
                    );
                    ctx.variables(BigDecimal.ZERO, BigDecimal.ZERO);
                }
        );
    }

    @Test
    public void testSubQueryWithAny() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.id().eq(
                                    AuthorTable.createSubQuery(q, (sq, author) -> {
                                        sq.where(author.firstName().in("Alex", "Bill"));
                                        return sq.select(author.books().id());
                                    }).any()
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where tb_1.ID = any(" +
                                        "select tb_3.BOOK_ID " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_2.FIRST_NAME in (?, ?)" +
                                    ")"
                    );
                    ctx.variables("Alex", "Bill");
                }
        );
    }

    @Test
    public void testSubQueryWithAll() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.id().eq(
                                    AuthorTable.createSubQuery(q, (sq, author) -> {
                                        sq.where(author.firstName().in("Alex", "Bill"));
                                        return sq.select(author.books().id());
                                    }).all()
                            )
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where tb_1.ID = all(" +
                                        "select tb_3.BOOK_ID " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_2.FIRST_NAME in (?, ?)" +
                                    ")"
                    );
                    ctx.variables("Alex", "Bill");
                }
        );
    }
}
