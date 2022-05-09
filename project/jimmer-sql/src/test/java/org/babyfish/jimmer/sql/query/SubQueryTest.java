package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

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
                                    BookTable.createSubQuery(query, (subQuery, book2) -> {
                                        return subQuery.select(book.price().avg());
                                    })
                            )
                    );
                    return query.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 where tb_1.PRICE > (" +
                                        "select avg(tb_1.PRICE) from BOOK as tb_2" +
                                    ")"
                    );
                }
        );
    }
}
