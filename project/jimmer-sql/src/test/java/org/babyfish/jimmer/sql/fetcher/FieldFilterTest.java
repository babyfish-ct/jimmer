package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class FieldFilterTest extends AbstractQueryTest {

    @Test
    public void testByOneToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .name()
                                            .books(
                                                    BookFetcher.$.name().edition(),
                                                    it -> it.filter(args -> {
                                                        BookTable book = args.getTable();
                                                        args.where(
                                                                Expression.tuple(book.name(), book.edition()).in(
                                                                        args.createSubQuery(BookTable.$)
                                                                                .where(BookTable.$.store().id().in(args.getKeys()))
                                                                                .groupBy(BookTable.$.name())
                                                                                .select(BookTable.$.name(), BookTable.$.edition().max())
                                                                )
                                                        );
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from BOOK_STORE tb_1_");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.STORE_ID, " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID in (?, ?) " +
                                    "and (tb_1_.NAME, tb_1_.EDITION) in (" +
                                        "select tb_3_.NAME, max(tb_3_.EDITION) " +
                                        "from BOOK tb_3_ " +
                                        "where tb_3_.STORE_ID in (?, ?) " +
                                        "group by tb_3_.NAME" +
                                    ")");
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\",\"name\":\"O'REILLY\"," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\",\"name\":\"Learning GraphQL\",\"edition\":3}," +
                                    "--->--->--->{\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\",\"name\":\"Effective TypeScript\",\"edition\":3}," +
                                    "--->--->--->{\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\",\"name\":\"Programming TypeScript\",\"edition\":3}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\",\"name\":\"GraphQL in Action\",\"edition\":3}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]");
                }
        );
    }

    @Test
    public void testByInverseManyToMany() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    AuthorFetcher.$
                                            .firstName()
                                            .lastName()
                                            .books(
                                                    BookFetcher.$.name().edition(),
                                                    it -> it.filter(args -> {
                                                        BookTable book = args.getTable();
                                                        args.where(
                                                                Expression.tuple(book.name(), book.edition()).in(
                                                                        args.createSubQuery(BookTableEx.$)
                                                                                .where(BookTableEx.$.authors().id().in(args.getKeys()))
                                                                                .groupBy(BookTableEx.$.name())
                                                                                .select(BookTableEx.$.name(), BookTableEx.$.edition().max())
                                                                )
                                                        );
                                                    })
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME from AUTHOR tb_1_");
                    ctx.statement(1).sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where " +
                                    "--->tb_2_.AUTHOR_ID in (?, ?, ?, ?, ?) " +
                                    "and " +
                                    "(tb_1_.NAME, tb_1_.EDITION) in (" +
                                    "--->select tb_4_.NAME, max(tb_4_.EDITION) " +
                                    "--->from BOOK tb_4_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.BOOK_ID " +
                                    "--->where tb_5_.AUTHOR_ID in (?, ?, ?, ?, ?) " +
                                    "--->group by tb_4_.NAME" +
                                    ")"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }
}
