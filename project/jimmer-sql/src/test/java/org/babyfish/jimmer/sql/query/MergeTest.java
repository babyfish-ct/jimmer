package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
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

    @Test
    public void testSubQuery() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                getSqlClient()
                                        .createSubQuery(author)
                                        .where(
                                                author.books().id().eq(table.id()),
                                                author.firstName().ilike("e")
                                        )
                                        .select(Expression.constant(1L))
                                        .minus(
                                                getSqlClient()
                                                        .createSubQuery(author)
                                                        .where(
                                                                author.books().id().eq(table.id()),
                                                                author.lastName().eq("Banks")
                                                        )
                                                        .select(Expression.constant(1L))
                                        )
                                        .exists()
                        )
                        .orderBy(table.name().asc(), table.edition().desc())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_2_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ " +
                                    "--->--->--->on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->--->where " +
                                    "--->--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->--->and " +
                                    "--->--->--->lower(tb_2_.FIRST_NAME) like ?" +
                                    "--->) minus (" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_5_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ " +
                                    "--->--->--->on tb_5_.ID = tb_6_.AUTHOR_ID " +
                                    "--->--->where tb_6_.BOOK_ID = tb_1_.ID " +
                                    "--->--->--->and tb_5_.LAST_NAME = ?" +
                                    "--->)" +
                                    ") " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
