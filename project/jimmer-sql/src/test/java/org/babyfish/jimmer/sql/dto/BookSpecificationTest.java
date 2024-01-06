package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.dto.BookSpecification;
import org.babyfish.jimmer.sql.model.dto.BookSpecification2;
import org.babyfish.jimmer.sql.model.dto.BookSpecification3;
import org.babyfish.jimmer.sql.model.dto.BookSpecification4;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
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
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_3_ " +
                                    "--->on tb_1_.ID = tb_3_.BOOK_ID " +
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
                                    "--->tb_1_.STORE_ID not in (?, ?) " +
                                    "and " +
                                    "--->tb_3_.AUTHOR_ID in (?, ?, ?) " +
                                    "and " +
                                    "--->tb_3_.AUTHOR_ID not in (?, ?) " +
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
}
