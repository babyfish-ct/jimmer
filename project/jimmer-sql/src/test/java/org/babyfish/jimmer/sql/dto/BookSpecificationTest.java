package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class BookSpecificationTest extends AbstractQueryTest {

    private static final BookTable table = BookTable.$;

    @Test
    public void testSpecificationWithoutNullity() {
        BookSpecification specification = new BookSpecification();
        specification.setStoreNotNull(true);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID is not null"
                    );
                }
        );
    }

    @Test
    public void testSpecification2WithoutValues() {
        BookSpecification2 specification = new BookSpecification2();
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "order by tb_1_.ID asc"
                    );
                }
        );
    }

    @Test
    public void testSpecification2WithValues() {
        BookSpecification2 specification = new BookSpecification2();
        specification.setIds(
                Arrays.asList(
                        Constants.learningGraphQLId1,
                        Constants.learningGraphQLId2,
                        Constants.learningGraphQLId3,
                        Constants.graphQLInActionId1,
                        Constants.graphQLInActionId2,
                        Constants.graphQLInActionId3
                )
        );
        specification.setName("GraphQL in Action");
        specification.setEdition(3);
        specification.setMinPrice(new BigDecimal(20));
        specification.setMaxPrice(new BigDecimal(50));
        specification.setStoreIds(Arrays.asList(Constants.oreillyId, Constants.manningId));
        specification.setExcludedStoreIds(
                Arrays.asList(
                        UUID.fromString("abcdabcd-abcd-abcd-abcd-abcdabcdabcd"),
                        UUID.fromString("dbcadbca-dbca-dbca-dbca-dbcadbcadbca")
                )
        );
        specification.setAuthorIds(Arrays.asList(Constants.alexId, Constants.eveId, Constants.borisId));
        specification.setExcludedAuthorIds(
                Arrays.asList(
                        UUID.fromString("cdefcdef-cdef-cdef-cdef-cdefcdefcdef"),
                        UUID.fromString("fedcfedc-fedc-fedc-fedc-fedcfedcfedc")
                )
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where " +
                                    "--->tb_1_.NAME = ? " +
                                    "and " +
                                    "--->tb_1_.EDITION = ? " +
                                    "and " +
                                    "--->tb_1_.ID in (?, ?, ?, ?, ?, ?) " +
                                    "and " +
                                    "--->tb_1_.PRICE >= ? " +
                                    "and " +
                                    "--->tb_1_.PRICE <= ? " +
                                    "and " +
                                    "--->tb_1_.STORE_ID in (?, ?) " +
                                    "and " +
                                    "--->not exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from BOOK_STORE tb_3_ " +
                                    "--->--->inner join BOOK tb_4_ on tb_3_.ID = tb_4_.STORE_ID " +
                                    "--->--->where tb_4_.ID = tb_1_.ID and tb_3_.ID in (?, ?)" +
                                    "--->) " +
                                    "and " +
                                    "--->exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_5_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ on tb_5_.ID = tb_6_.AUTHOR_ID " +
                                    "--->--->where tb_6_.BOOK_ID = tb_1_.ID and tb_5_.ID in (?, ?, ?)" +
                                    "--->) " +
                                    "and " +
                                    "--->not exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_8_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.AUTHOR_ID " +
                                    "--->--->where tb_9_.BOOK_ID = tb_1_.ID and tb_8_.ID in (?, ?)" +
                                    "--->) " +
                                    "order by tb_1_.ID asc"
                    ).variables(
                            "GraphQL in Action",
                            3,
                            Constants.learningGraphQLId1,
                            Constants.learningGraphQLId2,
                            Constants.learningGraphQLId3,
                            Constants.graphQLInActionId1,
                            Constants.graphQLInActionId2,
                            Constants.graphQLInActionId3,
                            new BigDecimal(20),
                            new BigDecimal(50),
                            Constants.oreillyId,
                            Constants.manningId,
                            UUID.fromString("abcdabcd-abcd-abcd-abcd-abcdabcdabcd"),
                            UUID.fromString("dbcadbca-dbca-dbca-dbca-dbcadbcadbca"),
                            Constants.alexId,
                            Constants.eveId,
                            Constants.borisId,
                            UUID.fromString("cdefcdef-cdef-cdef-cdef-cdefcdefcdef"),
                            UUID.fromString("fedcfedc-fedc-fedc-fedc-fedcfedcfedc")
                    );
                }
        );
    }

    @Test
    public void testSpecification3WithoutValues() {
        BookSpecification3 specification = new BookSpecification3();
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "order by tb_1_.ID asc"
                    );
                }
        );
    }

    @Test
    public void testSpecification3WithValues() {
        BookSpecification3 specification = new BookSpecification3();
        specification.setIds(
                Arrays.asList(
                        Constants.learningGraphQLId1,
                        Constants.learningGraphQLId2,
                        Constants.learningGraphQLId3,
                        Constants.graphQLInActionId1,
                        Constants.graphQLInActionId2,
                        Constants.graphQLInActionId3
                )
        );
        specification.setEdition(3);
        specification.setMinPrice(new BigDecimal(20));
        specification.setMaxPrice(new BigDecimal(50));

        BookSpecification3.TargetOf_store store = new BookSpecification3.TargetOf_store();
        store.setMinName("A");
        store.setMaxName("X");
        store.setVersion(1);
        store.setWebsite("http://www.manning.com");
        specification.setStore(store);

        BookSpecification3.TargetOf_authors authors = new BookSpecification3.TargetOf_authors();
        authors.setId(Constants.borisId);
        authors.setGender(Gender.MALE);
        authors.setName("B");
        specification.setAuthors(authors);

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where " +
                                    "--->tb_1_.EDITION = ? " +
                                    "and " +
                                    "--->tb_1_.ID in (?, ?, ?, ?, ?, ?) " +
                                    "and " +
                                    "--->tb_1_.PRICE >= ? " +
                                    "and " +
                                    "--->tb_1_.PRICE <= ? " +
                                    "and " +
                                    "--->tb_2_.WEBSITE = ? " +
                                    "and " +
                                    "--->tb_2_.VERSION = ? " +
                                    "and " +
                                    "--->tb_2_.NAME >= ? " +
                                    "and " +
                                    "--->tb_2_.NAME <= ? " +
                                    "and exists(" +
                                    "--->select 1 from AUTHOR tb_3_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_1_.ID = tb_4_.BOOK_ID " +
                                    "--->and " +
                                    "--->--->tb_3_.ID = ? " +
                                    "--->and " +
                                    "--->--->tb_3_.GENDER = ? " +
                                    "--->and " +
                                    "--->--->(tb_3_.FIRST_NAME ilike ? or tb_3_.LAST_NAME ilike ?)" +
                                    ") order by tb_1_.ID asc"
                    ).variables(
                            3,
                            Constants.learningGraphQLId1,
                            Constants.learningGraphQLId2,
                            Constants.learningGraphQLId3,
                            Constants.graphQLInActionId1,
                            Constants.graphQLInActionId2,
                            Constants.graphQLInActionId3,
                            new BigDecimal(20),
                            new BigDecimal(50),
                            "http://www.manning.com",
                            1,
                            "A",
                            "X",
                            Constants.borisId,
                            "M",
                            "%b%",
                            "%b%"
                    );
                }
        );
    }

    @Test
    public void testSpecification4WithoutValues() {
        BookSpecification4 specification = new BookSpecification4();
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "order by tb_1_.ID asc"
                    );
                }
        );
    }

    @Test
    public void testSpecification4WithValues() {
        BookSpecification4 specification = new BookSpecification4();
        specification.setIds(
                Arrays.asList(
                        Constants.learningGraphQLId1,
                        Constants.learningGraphQLId2,
                        Constants.learningGraphQLId3,
                        Constants.graphQLInActionId1,
                        Constants.graphQLInActionId2,
                        Constants.graphQLInActionId3
                )
        );
        specification.setEdition(3);
        specification.setMinPrice(new BigDecimal(20));
        specification.setMaxPrice(new BigDecimal(50));

        specification.setParentMinName("A");
        specification.setParentMaxName("X");
        specification.setParentWebsite("http://www.manning.com");
        BookSpecification3.TargetOf_store store = new BookSpecification3.TargetOf_store();

        specification.setAuthorName("B");

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where " +
                                    "--->tb_1_.EDITION = ? " +
                                    "and " +
                                    "--->tb_1_.ID in (?, ?, ?, ?, ?, ?) " +
                                    "and " +
                                    "--->tb_1_.PRICE >= ? " +
                                    "and " +
                                    "--->tb_1_.PRICE <= ? " +
                                    "and " +
                                    "--->tb_2_.NAME >= ? " +
                                    "and " +
                                    "--->tb_2_.NAME <= ? " +
                                    "and " +
                                    "--->tb_2_.WEBSITE = ? " +
                                    "and exists(" +
                                    "--->select 1 from AUTHOR tb_3_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_1_.ID = tb_4_.BOOK_ID " +
                                    "--->and " +
                                    "--->--->(tb_3_.FIRST_NAME ilike ? or tb_3_.LAST_NAME ilike ?)" +
                                    ") order by tb_1_.ID asc"
                    ).variables(
                            3,
                            Constants.learningGraphQLId1,
                            Constants.learningGraphQLId2,
                            Constants.learningGraphQLId3,
                            Constants.graphQLInActionId1,
                            Constants.graphQLInActionId2,
                            Constants.graphQLInActionId3,
                            new BigDecimal(20),
                            new BigDecimal(50),
                            "A",
                            "X",
                            "http://www.manning.com",
                            "%b%",
                            "%b%"
                    );
                }
        );
    }

    @Test
    public void testSpecification5() {
        BookSpecification5 specification = new BookSpecification5();
        specification.setName("GraphQL");
        specification.setStoreId(Constants.manningId);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME ilike ? and tb_1_.STORE_ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81.00," +
                                    "--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIssue562() {
        BookStoreSpecificationForIssue562 specification = new BookStoreSpecificationForIssue562();
        specification.setName("E");
        specification.setBookName("G");
        specification.setBookAuthorFirstName("A");

        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.NAME ilike ? and " +
                                    "exists(" +
                                    "--->select 1 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->where tb_1_.ID = tb_2_.STORE_ID " +
                                    "--->and tb_2_.NAME ilike ? " +
                                    "--->and exists(" +
                                    "--->--->select 1 " +
                                    "--->--->from AUTHOR tb_4_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                                    "--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->where tb_2_.ID = tb_5_.BOOK_ID " +
                                    "--->--->and tb_4_.FIRST_NAME ilike ?" +
                                    "--->)" +
                                    ")"
                    );
                    ctx.rows("[" +
                            "--->{" +
                            "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                            "--->--->\"name\":\"O'REILLY\",\"website\":null,\"version\":0" +
                            "--->}" +
                            "]");
                }
        );
    }

    @Test
    public void testFeatureRequest813() {
        BookTable table = BookTable.$;
        BookSpecification7 spec = new BookSpecification7();
        spec.setAuthorIds(Arrays.asList(Constants.alexId, Constants.sammerId));
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(spec)
                        .orderBy(table.name())
                        .orderBy(table.edition().desc())
                        .select(
                                table.fetch(
                                        BookFetcher.$.name()
                                                .authors(
                                                        AuthorFetcher.$.firstName()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_1_.ID = tb_3_.BOOK_ID and tb_2_.ID in (?, ?)" +
                                    ") " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?, ?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\",\"firstName\":\"Samer\"}" +
                                    "--->]" +
                                    "},{" +
                                    "--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\",\"firstName\":\"Samer\"}" +
                                    "--->]" +
                                    "},{" +
                                    "--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\",\"firstName\":\"Samer\"}" +
                                    "--->]" +
                                    "},{" +
                                    "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\",\"firstName\":\"Alex\"}," +
                                    "--->--->{\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\",\"firstName\":\"Eve\"}" +
                                    "--->]" +
                                    "},{" +
                                    "--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\",\"firstName\":\"Alex\"}," +
                                    "--->--->{\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\",\"firstName\":\"Eve\"}" +
                                    "--->]" +
                                    "},{" +
                                    "--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"authors\":[" +
                                    "--->--->{\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\",\"firstName\":\"Alex\"}," +
                                    "--->--->{\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\",\"firstName\":\"Eve\"}" +
                                    "--->]" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testIssue1108() {
        BookSpecificationForIssue1108 spec = new BookSpecificationForIssue1108();
        spec.setAuthorIds(Arrays.asList(Constants.alexId, Constants.borisId));
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(spec)
                        .where(table.edition().eq(3))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where exists(" +
                                    "--->select 1 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on " +
                                    "--->--->tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID and tb_2_.ID in (?, ?)" +
                                    ") and tb_1_.EDITION = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":51.00," +
                                    "--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "},{" +
                                    "--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->\"name\":\"Programming TypeScript\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":48.00," +
                                    "--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "}]"
                    );
                }
        );
    }
}
