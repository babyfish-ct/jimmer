package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.table.PolymorphicTable;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.*;
import org.babyfish.jimmer.sql.model.inheritance.singletable.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SingleTableInheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(101L))
                        .select(table.fetch(ClientFetcher.$.name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(101L);
                    ctx.row(0, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(101L, row.id());
                        assertEquals("Bob", row.name());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithSubtypeBranches() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(100L, 101L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        ClientFetcher.$
                                                .name()
                                                .forSubtype(OrganizationFetcher.$.taxCode())
                                                .forSubtype(PersonFetcher.$.firstName().lastName())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, " +
                                    "tb_2_.TAX_CODE, tb_3_.FIRST_NAME, tb_3_.LAST_NAME " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "left join CLIENT tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", "Person", 100L, 101L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(100L, row.id());
                        assertEquals("Acme", row.name());
                        assertEquals("ACME-001", ((Organization) row).taxCode());
                        assertLoadState(row, "id", "name", "taxCode");
                    });
                    ctx.row(1, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(101L, row.id());
                        assertEquals("Bob", row.name());
                        assertEquals("Bob", ((Person) row).firstName());
                        assertEquals("Brown", ((Person) row).lastName());
                        assertLoadState(row, "id", "name", "firstName", "lastName");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherWithSelfSubtypeIsRejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ClientFetcher.$.forSubtype(ClientFetcher.$.name())
        );
        assertTrue(ex.getMessage().contains("not strict subtype"));
    }

    @Test
    public void testRootFetcherMaterializesSubtype() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(100L))
                        .select(table.fetch(ClientFetcher.$.type().name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(100L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(100L, row.id());
                        assertEquals("ORG", row.type());
                        assertEquals("Acme", row.name());
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
                        .where(table.id().eq(1000L))
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
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1000L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(100L);
                    ctx.row(0, row -> {
                        assertEquals(1000L, row.id());
                        assertEquals("Single root project", row.name());
                        assertLoadState(row, "id", "name", "client");
                        assertEquals(Organization.class, ((ImmutableSpi) row.client()).__type().getJavaClass());
                        assertEquals(100L, row.client().id());
                        assertEquals("Acme", row.client().name());
                        assertLoadState(row.client(), "id", "name");
                    });
                }
        );
    }

    @Test
    public void testEnumDiscriminatorRootFetcherMaterializesSubtypeWithoutUserDiscriminator() {
        EnumClientTable table = EnumClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(110L))
                        .select(table.fetch(EnumClientFetcher.$)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                    "from ENUM_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(110L);
                    ctx.row(0, row -> {
                        assertEquals(EnumOrganization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(110L, row.id());
                        assertLoadState(row, "id");
                    });
                }
        );
    }

    @Test
    public void testEnumDiscriminatorRootFetcherMaterializesInstantiableRootWithoutUserDiscriminator() {
        EnumClientTable table = EnumClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(111L))
                        .select(table.fetch(EnumClientFetcher.$.name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from ENUM_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(111L);
                    ctx.row(0, row -> {
                        assertEquals(EnumClient.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(111L, row.id());
                        assertEquals("Enum Root", row.name());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }

    @Test
    public void testEnumDiscriminatorRootFetcherMaterializesSubtype() {
        EnumClientTable table = EnumClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(110L))
                        .select(table.fetch(EnumClientFetcher.$.type())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                    "from ENUM_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(110L);
                    ctx.row(0, row -> {
                        assertEquals(EnumOrganization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(110L, row.id());
                        assertEquals(ClientType.ORG, row.type());
                    });
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
                            "select tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals("ORG", row.get_1());
                        assertEquals("Acme", row.get_2());
                        assertEquals("ACME-001", row.get_3());
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
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals(100L, row.get_1());
                        assertEquals("Acme", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(102L, row.get_1());
                        assertEquals("Umbrella", row.get_2());
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
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", "Person");
                    ctx.rows("[100,101,102]");
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
                                    "from CLIENT tb_1_ " +
                                    "order by tb_1_.ID asc"
                    );
                    ctx.row(0, row -> {
                        assertEquals(100L, row.get_1());
                        assertEquals("Acme", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(101L, row.get_1());
                        assertEquals("Bob", row.get_2());
                    });
                    ctx.row(2, row -> {
                        assertEquals(102L, row.get_1());
                        assertEquals("Umbrella", row.get_2());
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
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "where tb_2_.CLIENT_TYPE = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals(1000L, row.get_1());
                        assertEquals("Acme", row.get_2());
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
                        .where(table.id().in(Arrays.asList(100L, 101L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "inner join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 100L, 101L);
                    ctx.row(0, row -> {
                        assertEquals(100L, row.get_1());
                        assertEquals("ACME-001", row.get_2());
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
                        .where(table.id().in(Arrays.asList(100L, 101L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 100L, 101L);
                    ctx.row(0, row -> {
                        assertEquals(100L, row.get_1());
                        assertEquals("ACME-001", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(101L, row.get_1());
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
                        .where(table.id().in(Arrays.asList(1000L, 1002L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_3_.TAX_CODE " +
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "inner join CLIENT tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 1000L, 1002L);
                    ctx.row(0, row -> {
                        assertEquals(1000L, row.get_1());
                        assertEquals("ACME-001", row.get_2());
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
                        .where(table.id().in(Arrays.asList(1000L, 1002L)))
                        .orderBy(table.id())
                        .select(table.id(), organization.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_3_.TAX_CODE " +
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.CLIENT_ID = tb_2_.ID " +
                                    "left join CLIENT tb_3_ " +
                                    "on tb_2_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 1000L, 1002L);
                    ctx.row(0, row -> {
                        assertEquals(1000L, row.get_1());
                        assertEquals("ACME-001", row.get_2());
                    });
                    ctx.row(1, row -> {
                        assertEquals(1002L, row.get_1());
                        assertNull(row.get_2());
                    });
                }
        );
    }
}
