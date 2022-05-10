package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Test;

public class MergeTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                BookTable.createQuery(getSqlClient(), (q, book) -> {
                    q.where(
                            book.name().ilike("G"),
                            book.id().in(
                                    AuthorTable.createSubQuery(q, (sq, author) -> {
                                        sq.where(author.firstName().like("A"));
                                        return sq.select(author.books().id());
                                    })
                            )
                    );
                    return q.select(book);
                }).minus(
                        BookTable.createQuery(getSqlClient(), (q, book) -> {
                            q.where(
                                    book.name().ilike("F"),
                                    book.id().in(
                                            AuthorTable.createSubQuery(q, (sq, author) -> {
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
                            "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where lower(tb_1.NAME) like ? " +
                                    "and tb_1.ID in (" +
                                        "select tb_3.BOOK_ID " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_2.FIRST_NAME like ?" +
                                    ") " +
                                    "minus " +
                                    "select tb_1.ID, tb_1.NAME, tb_1.EDITION, tb_1.PRICE, tb_1.STORE_ID " +
                                    "from BOOK as tb_1 " +
                                    "where lower(tb_1.NAME) like ? " +
                                    "and tb_1.ID in (" +
                                        "select tb_3.BOOK_ID " +
                                        "from AUTHOR as tb_2 " +
                                        "inner join BOOK_AUTHOR_MAPPING as tb_3 on tb_2.ID = tb_3.AUTHOR_ID " +
                                        "where tb_2.FIRST_NAME like ?" +
                                    ")"
                    );
                    ctx.variables("%g%", "%A%", "%f%", "%C%");
                }
        );
    }
}
