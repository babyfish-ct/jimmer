package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.ClientType;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClientFetcher;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumClientTable;
import org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator.EnumOrganization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_TYPE " +
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
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_TYPE " +
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
}
