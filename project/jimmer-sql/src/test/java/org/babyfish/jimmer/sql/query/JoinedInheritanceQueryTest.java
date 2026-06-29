package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.table.PolymorphicTable;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class JoinedInheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(201L))
                        .select(table.fetch(ClientFetcher.$.name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(201L);
                    ctx.row(0, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(201L, row.id());
                        assertEquals("Alice", row.name());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithTypeBranches() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(200L, 201L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        ClientFetcher.$
                                                .name()
                                                .forType(OrganizationFetcher.$.taxCode())
                                                .forType(PersonFetcher.$.firstName().lastName())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, " +
                                    "tb_2_.TAX_CODE, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "left join JOINED_ORGANIZATION tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "left join JOINED_PERSON tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", "Person", 200L, 201L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(200L, row.id());
                        assertEquals("Globex", row.name());
                        assertEquals("GLOBEX-001", ((Organization) row).taxCode());
                        assertLoadState(row, "id", "name", "taxCode");
                    });
                    ctx.row(1, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(201L, row.id());
                        assertEquals("Alice", row.name());
                        assertEquals("Alice", ((Person) row).firstName());
                        assertEquals("Smith", ((Person) row).lastName());
                        assertLoadState(row, "id", "name", "firstName", "lastName");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithSubtypeAssociationBranch() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(200L, 201L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        ClientFetcher.$
                                                .name()
                                                .forType(
                                                        OrganizationFetcher.$.projects(OrganizationProjectFetcher.$.name())
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables(200L, 201L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_ORG_PROJECT tb_1_ " +
                                    "where tb_1_.ORGANIZATION_ID = ?"
                    ).variables(200L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertLoadState(row, "id", "name", "projects");
                        assertEquals(1, ((Organization) row).projects().size());
                        assertEquals("Joined organization project", ((Organization) row).projects().get(0).name());
                    });
                    ctx.row(1, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithSelfSubtypeIsRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ClientFetcher.$.forType(ClientFetcher.$.name())
        );
        assertTrue(ex.getMessage().contains("only strict subtype fetchers are supported currently"));
    }

    @Test
    public void testRootFetcherMaterializesSubtype() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(200L))
                        .select(table.fetch(ClientFetcher.$.type().name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(200L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(200L, row.id());
                        assertEquals("ORG", row.type());
                        assertEquals("Globex", row.name());
                    });
                }
        );
    }

    @Test
    public void testReferenceFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        ClientProjectTable table = ClientProjectTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(2000L))
                        .select(
                                table.fetch(
                                        ClientProjectFetcher.$
                                                .name()
                                                .client(ClientFetcher.$.name())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_ID " +
                                    "from JOINED_CLIENT_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(2000L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(200L);
                    ctx.row(0, row -> {
                        assertEquals(2000L, row.id());
                        assertEquals("Joined root project", row.name());
                        assertLoadState(row, "id", "name", "client");
                        assertEquals(Organization.class, ((ImmutableSpi) row.client()).__type().getJavaClass());
                        assertEquals(200L, row.client().id());
                        assertEquals("Globex", row.client().name());
                        assertLoadState(row.client(), "id", "name");
                    });
                }
        );
    }

    @Test
    public void testSubtypeTableSelection() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(200L))
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                    ).variables(200L, "ORG");
                    ctx.rows("[{\"type\":\"ORG\",\"id\":200,\"name\":\"Globex\",\"taxCode\":\"GLOBEX-001\"}]");
                }
        );
    }

    @Test
    public void testSubtypeQueryWithRootAndSubtypeFields() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.type(), table.name(), table.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1__sub.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals("ORG", row.get_1());
                        assertEquals("Globex", row.get_2());
                        assertEquals("GLOBEX-001", row.get_3());
                    });
                }
        );
    }

    @Test
    public void testSubtypeQueryWithRootFieldsOnly() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.type(), table.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals("ORG", row.get_1());
                        assertEquals("Globex", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testPolymorphicTableScope() {
        assertTrue(ClientTable.$ instanceof PolymorphicTable<?>);
        assertFalse(OrganizationTable.$ instanceof PolymorphicTable<?>);
    }

    @Test
    public void testInstanceOf() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.instanceOf(Organization.class))
                        .orderBy(table.id())
                        .select(table.id(), table.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals(200L, row.get_1());
                        assertEquals("Globex", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(202L, row.get_1());
                        assertEquals("Initech", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testInstanceOfRootRendersDiscriminatorPredicate() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.instanceOf(Client.class))
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", "Person");
                    ctx.rows("[200,201,202]");
                }
        );
    }

    @Test
    public void testTreatAsRootIsNoOp() {
        ClientTable table = ClientTable.$;
        ClientTable client = table.treatAs(ClientTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .orderBy(table.id())
                        .select(table.id(), client.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.row(0, row -> {
                        assertEquals(200L, row.get_1());
                        assertEquals("Globex", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(201L, row.get_1());
                        assertEquals("Alice", row.get_2());
                    });
                    ctx.row(2, row -> {
                        assertEquals(202L, row.get_1());
                        assertEquals("Initech", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testInstanceOfOnAssociationPath() {
        ClientProjectTable table = ClientProjectTable.$;
        ClientTable client = table.asTableEx().client(JoinType.LEFT);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(client.instanceOf(Organization.class))
                        .orderBy(table.id())
                        .select(table.id(), client.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.NAME " +
                                    "from JOINED_CLIENT_PROJECT tb_1_ " +
                                    "left join JOINED_CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "where tb_2_.CLIENT_TYPE = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals(2000L, row.get_1());
                        assertEquals("Globex", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testTreatAs() {
        ClientTable table = ClientTable.$;
        OrganizationTable organization = table.treatAs(OrganizationTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(200L, 201L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "inner join JOINED_ORGANIZATION tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 200L, 201L);
                    ctx.row(0, row -> {
                        assertEquals(200L, row.get_1());
                        assertEquals("GLOBEX-001", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testTryTreatAs() {
        ClientTable table = ClientTable.$;
        OrganizationTable organization = table.tryTreatAs(OrganizationTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(200L, 201L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "left join JOINED_ORGANIZATION tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 200L, 201L);
                    ctx.row(0, row -> {
                        assertEquals(200L, row.get_1());
                        assertEquals("GLOBEX-001", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(201L, row.get_1());
                        assertNull(row.get_2());
                    });
                }
        );
    }

    @Test
    public void testTreatAsOnNullableAssociationPath() {
        ClientProjectTable table = ClientProjectTable.$;
        OrganizationTable organization = table
                .asTableEx()
                .client(JoinType.LEFT)
                .treatAs(OrganizationTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(2000L, 2002L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_3_.TAX_CODE " +
                                    "from JOINED_CLIENT_PROJECT tb_1_ " +
                                    "left join JOINED_CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "inner join JOINED_ORGANIZATION tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 2000L, 2002L);
                    ctx.row(0, row -> {
                        assertEquals(2000L, row.get_1());
                        assertEquals("GLOBEX-001", row.get_2());
                    });
                }
        );
    }

    @Test
    public void testTryTreatAsOnNullableAssociationPath() {
        ClientProjectTable table = ClientProjectTable.$;
        OrganizationTable organization = table
                .asTableEx()
                .client(JoinType.LEFT)
                .tryTreatAs(OrganizationTable.class);
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(2000L, 2002L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_3_.TAX_CODE " +
                                    "from JOINED_CLIENT_PROJECT tb_1_ " +
                                    "left join JOINED_CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "left join JOINED_ORGANIZATION tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 2000L, 2002L);
                    ctx.row(0, row -> {
                        assertEquals(2000L, row.get_1());
                        assertEquals("GLOBEX-001", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(2002L, row.get_1());
                        assertNull(row.get_2());
                    });
                }
        );
    }
}
