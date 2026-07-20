package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.CacheImpl;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientFetcher;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationFetcher;
import org.babyfish.jimmer.sql.model.issue1252.TreeNode2;
import org.babyfish.jimmer.sql.model.issue1252.TreeNode2Fetcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.babyfish.jimmer.sql.common.Constants.*;

public class ObjectCacheTest extends AbstractQueryTest {

    private JSqlClient sqlClient;

    @BeforeEach
    public void initialize() {
        sqlClient = getSqlClient(builder -> {
            builder.setCaches(cfg ->
                    cfg.setCacheFactory(
                            new CacheFactory() {

                                @Override
                                public Cache<?, ?> createObjectCache(ImmutableType type) {
                                    return new CacheImpl<>(type);
                                }

                                @Override
                                public Cache<?, ?> createAssociatedIdCache(ImmutableProp prop) {
                                    return new CacheImpl<>(prop);
                                }

                                @Override
                                public Cache<?, List<?>> createAssociatedIdListCache(ImmutableProp prop) {
                                    return new CacheImpl<>(prop);
                                }

                                @Override
                                public Cache<?, ?> createResolverCache(@NotNull ImmutableProp prop) {
                                    return new CacheImpl<>(prop);
                                }
                            }
                    )
            );
        });
    }

    @Test
    public void testObject() {
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> {
                        return sqlClient
                                .getEntities()
                                .forConnection(con)
                                .findById(BookStore.class, oreillyId);
                    }, ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                            "from BOOK_STORE tb_1_ " +
                                            "where tb_1_.ID = ?"
                            );
                            ctx.variables(oreillyId);
                        }
                        ctx.rows(
                                "[" +
                                        "--->{" +
                                        "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                        "--->--->\"name\":\"O'REILLY\"," +
                                        "--->--->\"website\":null," +
                                        "--->--->\"version\":0" +
                                        "--->}" +
                                        "]"
                        );
                    }
            );
        }
    }

    @Test
    public void testCalculatedAssociation() {
        BookStoreTable table = BookStoreTable.$;
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            executeAndExpect(
                    sqlClient
                            .createQuery(table)
                            .select(
                                    table.fetch(
                                            BookStoreFetcher.$
                                                    .allScalarFields()
                                                    .newestBooks(
                                                            BookFetcher.$
                                                                    .allScalarFields()
                                                    )
                                    )
                            ),
                    ctx -> {
                        ctx.sql("select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION from BOOK_STORE tb_1_");
                        if (useSql) {
                            ctx.statement(1).sql("select tb_1_.ID, tb_2_.ID from BOOK_STORE tb_1_ inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID where (tb_2_.NAME, tb_2_.EDITION) in (select tb_3_.NAME, max(tb_3_.EDITION) from BOOK tb_3_ where tb_3_.STORE_ID in (?, ?) group by tb_3_.NAME)");
                            ctx.statement(2).sql("select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID from BOOK tb_1_ where tb_1_.ID in (?, ?, ?, ?)");
                        }
                    }
            );
        }
    }

    @Test
    public void testIssue1221ByObject() {
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> {
                        return sqlClient
                                .getEntities()
                                .forConnection(con)
                                .findById(
                                        AuthorFetcher.$.fullName(),
                                        alexId
                                );
                    }, ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, " +
                                            "tb_1_.FIRST_NAME, " +
                                            "tb_1_.LAST_NAME, " +
                                            "tb_1_.GENDER, " +
                                            "length(tb_1_.FIRST_NAME) + length(tb_1_.LAST_NAME), " +
                                            "concat(tb_1_.FIRST_NAME, ' ', tb_1_.LAST_NAME) " +
                                            "from AUTHOR tb_1_ where tb_1_.ID = ?"
                            );
                        }
                        ctx.rows(
                                "[{" +
                                        "--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                        "--->\"fullName\":\"Alex Banks\"" +
                                        "}]"
                        );
                    }
            );
        }
    }

    @Test
    public void testIssue1221ByAssociation() {
        for (int i = 0; i < 2; i++) {
            boolean useSql = i == 0;
            connectAndExpect(
                    con -> {
                        return sqlClient
                                .getEntities()
                                .forConnection(con)
                                .findById(
                                        BookFetcher.$.storeId(),
                                        graphQLInActionId3
                                );
                    }, ctx -> {
                        if (useSql) {
                            ctx.sql(
                                    "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                            "from BOOK tb_1_ " +
                                            "where tb_1_.ID = ?"
                            );
                        }
                        ctx.rows(
                                "[{" +
                                        "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                        "--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "}]"
                        );
                    }
            );
        }
    }

    @Test
    public void testIssue1252() {
        connectAndExpect(
                con -> {
                    return sqlClient
                            .getEntities()
                            .forConnection(con)
                            .findById(
                                    TreeNode2Fetcher.$.name()
                                            .childNodes(
                                                    TreeNode2Fetcher.$
                                                            .name()
                                            ),
                                    1L
                            );
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE_2 tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID " +
                                    "from TREE_NODE_2 tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE_2 tb_1_ " +
                                    "where tb_1_.NODE_ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"name\":\"Home\"," +
                                    "--->\"childNodes\":[" +
                                    "--->--->{\"id\":9,\"name\":\"Clothing\"}," +
                                    "--->--->{\"id\":2,\"name\":\"Food\"}]" +
                                    "}]"
                    );
                }
        );
        connectAndExpect(
                con -> {
                    updateTreeNodeName(con, "clothing");
                    sqlClient.getCaches().getObjectCache(TreeNode2.class).delete(9L);
                    return sqlClient
                            .getEntities()
                            .forConnection(con)
                            .findById(
                                    TreeNode2Fetcher.$.name()
                                            .childNodes(
                                                    TreeNode2Fetcher.$
                                                            .name()
                                            ),
                                    1L
                            );
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE_2 tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"name\":\"Home\"," +
                                    "--->\"childNodes\":[" +
                                    "--->--->{\"id\":2,\"name\":\"Food\"}," +
                                    "--->--->{\"id\":9,\"name\":\"clothing\"}]" +
                                    "}]"
                    );
                }
        );
    }

    private static void updateTreeNodeName(Connection con, String name) {
        try (PreparedStatement stmt = con.prepareStatement(
                "update tree_node set name = ? where node_id = ?"
        )) {
            stmt.setString(1, name);
            stmt.setLong(2, 9L);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            Assertions.fail("SQL error", ex);
        }
    }

    @Test
    public void testPolymorphicRootFetcher() {
        connectAndExpect(con -> {
            List<Client> clients = sqlClient
                    .getEntities()
                    .forConnection(con)
                    .findByIds(ClientFetcher.$.name(), singletonList(200L));
            Assertions.assertEquals(1, clients.size());
            Client client = clients.get(0);
            Assertions.assertEquals(Organization.class, ((ImmutableSpi) client).__type().getJavaClass());
            Assertions.assertEquals("Globex", client.name());
            Assertions.assertFalse(client instanceof DraftSpi);
            return clients;
        }, ctx -> {
            ctx.sql(
                    "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.DESCRIPTION " +
                            "from JOINED_CLIENT tb_1_ where tb_1_.ID = ?"
            );
            ctx.rows("[{\"id\":200,\"name\":\"Globex\"}]");
        });
    }

    @Test
    public void testIssue1154WithId() {
        for (int i = 0; i < 2; i++) {
            final boolean useSql = i == 0;
            connectAndExpect(con -> {
                Organization org = sqlClient
                        .getEntities()
                        .forConnection(con)
                        .findById(OrganizationFetcher.$.allScalarFields(), 200L);
                Assertions.assertFalse(org instanceof DraftSpi);
                return org;
            }, ctx -> {
                if (useSql) {
                    ctx.sql(
                            "select " +
                            "--->tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.DESCRIPTION, tb_1__sub.TAX_CODE, tb_1__sub.STATUS " +
                            "from JOINED_CLIENT tb_1_ " +
                            "--->inner join JOINED_ORGANIZATION tb_1__sub on tb_1_.ID = tb_1__sub.ID " +
                            "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                    );
                }
                if (useSql) { // BUG！discriminator is loaded first time, but is not loaded at second time.
                    ctx.rows(
                            "[{" +
                            "--->\"type\":\"ORG\"," +
                            "--->\"id\":200," +
                            "--->\"name\":\"Globex\"," +
                            "--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                            "--->\"taxCode\":\"GLOBEX-001\"," +
                            "--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                            "}]"
                    );
                } else {
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":200," +
                                    "--->\"name\":\"Globex\"," +
                                    "--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->\"taxCode\":\"GLOBEX-001\"," +
                                    "--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "}]"
                    );
                }
            });
        }
    }

    @Test
    public void testIssue1154WithIds() {
        for (int i = 0; i < 2; i++) {
            final boolean useSql = i == 0;
            connectAndExpect(con -> {
                List<Organization> orgs = sqlClient
                        .getEntities()
                        .forConnection(con)
                        .findByIds(OrganizationFetcher.$.allScalarFields(), Arrays.asList(200L, 202L));
                for (Organization org : orgs) {
                    Assertions.assertFalse(org instanceof DraftSpi);
                }
                return orgs;
            }, ctx -> {
                if (useSql) {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.DESCRIPTION, tb_1__sub.TAX_CODE, tb_1__sub.STATUS " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "--->inner join JOINED_ORGANIZATION tb_1__sub on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.ID in (?, ?) and tb_1_.CLIENT_TYPE = ?"
                    );
                }
                if (useSql) { // BUG！discriminator is loaded first time, but is not loaded at second time.
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"type\":\"ORG\"," +
                                    "--->--->\"id\":200," +
                                    "--->--->\"name\":\"Globex\"," +
                                    "--->--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->--->\"taxCode\":\"GLOBEX-001\"," +
                                    "--->--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "--->},{" +
                                    "--->--->\"type\":\"ORG\"," +
                                    "--->--->\"id\":202," +
                                    "--->--->\"name\":\"Initech\"," +
                                    "--->--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->--->\"taxCode\":\"INI-001\"," +
                                    "--->--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "--->}" +
                                    "]"
                    );
                } else {
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":200," +
                                    "--->--->\"name\":\"Globex\"," +
                                    "--->--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->--->\"taxCode\":\"GLOBEX-001\"," +
                                    "--->--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "--->},{" +
                                    "--->--->\"id\":202," +
                                    "--->--->\"name\":\"Initech\"," +
                                    "--->--->\"description\":\"DEFAULT_CLIENT_DESCRIPTION\"," +
                                    "--->--->\"taxCode\":\"INI-001\"," +
                                    "--->--->\"status\":\"DEFAULT_ORGANIZATION_STATUS\"" +
                                    "--->}" +
                                    "]"
                    );
                }
            });
        }
    }
}
