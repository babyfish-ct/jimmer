package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class CursorTest extends AbstractQueryTest {

    @Test
    public void cursor() {
        connectAndExpect(
                con -> {
                    List<Book> books = new ArrayList<>();
                    getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                        return q
                                .where(book.edition().eq(3))
                                .orderBy(book.name().asc())
                                .select(
                                        book.fetch(
                                                BookFetcher.$.allScalarFields()
                                                        .store(
                                                                BookStoreFetcher.$.allScalarFields()
                                                        )
                                                        .authors(
                                                                AuthorFetcher.$.allScalarFields()
                                                        )
                                        )
                                );
                    }).forEach(con, 3, book -> {
                        books.add(book);
                    });
                    return books;
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ? " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":88.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                                    "--->--->--->--->\"firstName\":\"Dan\"," +
                                    "--->--->--->--->\"lastName\":\"Vanderkam\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":48.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"," +
                                    "--->--->--->--->\"firstName\":\"Boris\"," +
                                    "--->--->--->--->\"lastName\":\"Cherny\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
