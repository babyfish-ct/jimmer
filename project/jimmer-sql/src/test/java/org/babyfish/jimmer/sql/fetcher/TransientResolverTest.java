package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class TransientResolverTest extends AbstractQueryTest {

    @Test
    public void testAvgPrice() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .avgPrice()
                            )
                    );
                }),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    it.statement(1).sql(
                            "select tb_1_.ID, coalesce(avg(tb_2_.PRICE), ?) " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "left join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "group by tb_1_.ID"
                    ).variables(BigDecimal.ZERO, oreillyId, manningId);
                    it.rows(
                            "[{" +
                                    "--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->\"name\":\"O'REILLY\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":0," +
                                    "--->\"avgPrice\":58.500000000000" +
                                    "},{" +
                                    "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->\"name\":\"MANNING\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":0," +
                                    "--->\"avgPrice\":80.333333333333" +
                                    "}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMostPopularAuthor() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .mostPopularAuthor()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.NAME, " +
                                    "tb_1_.WEBSITE, " +
                                    "tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_3_.AUTHOR_ID, count(tb_2_.ID) " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "group by tb_1_.ID, tb_3_.AUTHOR_ID " +
                                    "order by count(tb_3_.AUTHOR_ID) desc, tb_3_.AUTHOR_ID asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"mostPopularAuthor\":{" +
                                    "--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"mostPopularAuthor\":{" +
                                    "--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMostPopularAuthorWithDeepFetcher() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .mostPopularAuthor(
                                                    AuthorFetcher.$
                                                            .allScalarFields()
                                                            .books(
                                                                    BookFetcher.$.allScalarFields()
                                                            )
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.NAME, " +
                                    "tb_1_.WEBSITE, " +
                                    "tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_3_.AUTHOR_ID, count(tb_2_.ID) " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "group by tb_1_.ID, tb_3_.AUTHOR_ID " +
                                    "order by count(tb_3_.AUTHOR_ID) desc, tb_3_.AUTHOR_ID asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ where tb_1_.ID in (?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select tb_2_.AUTHOR_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"mostPopularAuthor\":{" +
                                    "--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->\"gender\":\"MALE\"," +
                                    "--->--->--->\"books\":[" +
                                    "--->--->--->--->{" +
                                    "--->--->--->--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->--->\"edition\":1,\"price\":50.00" +
                                    "--->--->--->--->},{" +
                                    "--->--->--->--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->--->\"edition\":2,\"price\":55.00" +
                                    "--->--->--->--->},{" +
                                    "--->--->--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->--->\"edition\":3,\"price\":51.00" +
                                    "--->--->--->--->}" +
                                    "--->--->--->]" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"mostPopularAuthor\":{" +
                                    "--->--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->--->\"firstName\":\"Samer\"," +
                                    "--->--->--->\"lastName\":\"Buna\"," +
                                    "--->--->--->\"gender\":\"MALE\"," +
                                    "--->--->--->\"books\":[" +
                                    "--->--->--->--->{" +
                                    "--->--->--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->--->\"edition\":1,\"price\":80.00" +
                                    "--->--->--->--->},{" +
                                    "--->--->--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->--->\"edition\":2,\"price\":81.00" +
                                    "--->--->--->--->},{" +
                                    "--->--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->--->\"price\":80.00" +
                                    "--->--->--->--->}" +
                                    "--->--->--->]" +
                                    "--->--->}" +
                                    "--->}" +
                                    "--->]"
                    );
                }
        );
    }

    @Test
    public void testNewestBooks() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .newestBooks(
                                                    BookFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_2_.ID " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where (tb_2_.NAME, tb_2_.EDITION) in (" +
                                    "--->select tb_3_.NAME, max(tb_3_.EDITION) " +
                                    "--->from BOOK as tb_3_ " +
                                    "--->where tb_3_.STORE_ID in (?, ?) " +
                                    "--->group by tb_3_.NAME" +
                                    ")"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK as tb_1_ where tb_1_.ID in (?, ?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null,\"version\":0," +
                                    "--->--->\"newestBooks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":51.00" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":88.00" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":48.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"newestBooks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":80.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testNewestBooksWithPropFilter() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    return q.select(
                            store.fetch(
                                    BookStoreFetcher.$
                                            .allScalarFields()
                                            .newestBooks(
                                                    BookFetcher.$.allScalarFields(),
                                                    cfg -> cfg.filter(it ->
                                                            it.orderBy(it.getTable().name())
                                                    )
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_2_.ID " +
                                    "from BOOK_STORE as tb_1_ " +
                                    "inner join BOOK as tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where (tb_2_.NAME, tb_2_.EDITION) in (" +
                                    "--->select tb_3_.NAME, max(tb_3_.EDITION) " +
                                    "--->from BOOK as tb_3_ " +
                                    "--->where tb_3_.STORE_ID in (?, ?) " +
                                    "--->group by tb_3_.NAME" +
                                    ")"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK as tb_1_ where tb_1_.ID in (?, ?, ?, ?) " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null,\"version\":0," +
                                    "--->--->\"newestBooks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":88.00" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":51.00" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":48.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"newestBooks\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":80.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
