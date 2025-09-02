package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.dto.BookViewForTupleTest;
import org.junit.jupiter.api.Test;

public class TypedTupleTest extends AbstractQueryTest {

    @Test
    public void testEntityTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                EntityTupleMapper
                                        .book(
                                                table.fetch(
                                                        BookFetcher.$.name()
                                                                .store(
                                                                        BookStoreFetcher.$.name()
                                                                )
                                                )
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testLombokEntityTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                EntityTupleMapper
                                        .book(
                                                table.fetch(
                                                        BookFetcher.$.name()
                                                                .store(
                                                                        BookStoreFetcher.$.name()
                                                                )
                                                )
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testDtoTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                DtoTupleMapper
                                        .book(
                                                table.fetch(BookViewForTupleTest.class)
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testLombokDtoTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                LombokDtoTupleMapper
                                        .book(
                                                table.fetch(BookViewForTupleTest.class)
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }
}
