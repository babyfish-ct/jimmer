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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query.where(
                                book.id().in(
                                        AuthorTable.createSubQuery(query, (subQuery, author) ->
                                                subQuery
                                                        .where(author.firstName().eq("Alex"))
                                                        .select(author.books().id())
                                        )
                                )
                        ).select(book)
                ),
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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query
                                .where(
                                        Expression.tuple(book.name(), book.price()).in(
                                                BookTable.createSubQuery(query, (subQuery, book2) ->
                                                        subQuery
                                                                .groupBy(book2.name())
                                                                .select(
                                                                        book2.name(),
                                                                        book2.price().max()
                                                                )
                                                )
                                        )
                                )
                                .select(book)
                ),
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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query.where(
                                AuthorTable.createWildSubQuery(query, (subQuery, author) ->
                                        subQuery
                                                .where(
                                                        book.eq(author.books()),
                                                        author.firstName().eq("Alex")
                                                )
                                ).exists()
                        ).select(book)
                ),
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
                BookTable.createQuery(getSqlClient(), (query, book) ->
                        query.where(
                                AuthorTable.createSubQuery(query, (subQuery, author) ->
                                        subQuery
                                                .where(
                                                        book.eq(author.books()),
                                                        author.firstName().eq("Alex")
                                                )
                                                .select(author)
                                ).exists()
                        ).select(book)
                ),
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
                BookTable.createQuery(getSqlClient(), (query, book) -> {
                    query.where(
                            book.price().gt(
                                    BookTable.createSubQuery(query, (subQuery, book2) ->
                                        subQuery.select(book.price().avg().coalesce(BigDecimal.ZERO))
                                    )
                            )
                    );
                    return query.select(book);
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
                BookStoreTable.createQuery(getSqlClient(), (query, store) -> {
                    TypedSubQuery<BigDecimal> sq =
                            BookTable.createSubQuery(query, (subQuery, book) -> {
                                subQuery.where(store.eq(book.store()));
                                return subQuery.select(
                                        book.price().avg().coalesce(BigDecimal.ZERO)
                                );
                            });
                    query.orderBy(sq, OrderMode.DESC);
                    return query.select(store, sq);
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
                BookTable.createQuery(getSqlClient(), (query, book) -> {
                    query.where(
                            book.id().eq(
                                    AuthorTable.createSubQuery(query, (subQuery, author) -> {
                                        subQuery.where(author.firstName().in("Alex", "Bill"));
                                        return subQuery.select(author.books().id());
                                    }).any()
                            )
                    );
                    return query.select(book);
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
                BookTable.createQuery(getSqlClient(), (query, book) -> {
                    query.where(
                            book.id().eq(
                                    AuthorTable.createSubQuery(query, (subQuery, author) -> {
                                        subQuery.where(author.firstName().in("Alex", "Bill"));
                                        return subQuery.select(author.books().id());
                                    }).all()
                            )
                    );
                    return query.select(book);
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
