package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class MergeTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.name().ilike("G"),
                            book.id().in(
                                    getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                        sq.where(author.firstName().like("A"));
                                        return sq.select(author.books().id());
                                    })
                            )
                    );
                    return q.select(book);
                }).minus(
                        getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                            q.where(
                                    book.name().ilike("F"),
                                    book.id().in(
                                            getLambdaClient().createSubQuery(q, AuthorTableEx.class, (sq, author) -> {
                                                sq.where(author.firstName().like("C"));
                                                return sq.select(author.books().id());
                                            })
                                    )
                            );
                            return q.select(book);
                        })
                ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where lower(tb_1_.NAME) like ? " +
                                    "and tb_1_.ID in (" +
                                        "select tb_3_.BOOK_ID " +
                                        "from AUTHOR tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_2_.FIRST_NAME like ?" +
                                    ") " +
                                    "minus " +
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where lower(tb_1_.NAME) like ? " +
                                    "and tb_1_.ID in (" +
                                        "select tb_3_.BOOK_ID " +
                                        "from AUTHOR tb_2_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "where tb_2_.FIRST_NAME like ?" +
                                    ")"
                    );
                    ctx.variables("%g%", "%A%", "%f%", "%C%");
                }
        );
    }
}
