package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

public class AssociationPagingTest extends AbstractQueryTest {

    @Test
    public void testOneToMany() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.name().asc())
                        .select(
                                table.fetch(
                                        BookStoreFetcher.$
                                                .allScalarFields()
                                                .books(
                                                        BookFetcher.$
                                                                .allScalarFields(),
                                                        it -> it.limit(2, 2)
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(1).sql(
                            "(" +
                                    "--->select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "--->from BOOK tb_1_ " +
                                    "--->where tb_1_.STORE_ID = ? " +
                                    "--->limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_1_.STORE_ID, tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "--->from BOOK tb_1_ " +
                                    "--->where tb_1_.STORE_ID = ? " +
                                    "--->limit ? offset ?" +
                                    ")"
                    ).variables(Constants.manningId, 2, 2L, Constants.oreillyId, 2, 2L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":80.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->--->--->\"edition\":3," +
                                    "--->--->--->--->\"price\":51.00" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"8f30bc8a-49f9-481d-beca-5fe2d147c831\"," +
                                    "--->--->--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->--->--->\"edition\":1," +
                                    "--->--->--->--->\"price\":73.00" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testManyToMany() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().ilike("GraphQL"))
                        .orderBy(table.name().asc())
                        .orderBy(table.edition().desc())
                        .select(
                                table.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .authors(
                                                        AuthorFetcher.$
                                                                .allScalarFields(),
                                                        it -> it.limit(1, 1)
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME ilike ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc"
                    );
                    ctx.statement(1).sql(
                            "(" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? " +
                                    "--->limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? limit ? offset ?" +
                                    ") union all (" +
                                    "--->select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "--->from AUTHOR tb_1_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "--->where tb_2_.BOOK_ID = ? limit ? offset ?" +
                                    ")"
                    )
                    .variables(
                            Constants.graphQLInActionId3, 1, 1L,
                            Constants.graphQLInActionId2, 1, 1L,
                            Constants.graphQLInActionId1, 1, 1L,
                            Constants.learningGraphQLId3, 1, 1L,
                            Constants.learningGraphQLId2, 1, 1L,
                            Constants.learningGraphQLId1, 1, 1L
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00,\"authors\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81.00,\"authors\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1,\"price\":80.00," +
                                    "--->--->\"authors\":[]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00,\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00,\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
