package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedCascadeInheritanceFetcherTest extends AbstractQueryTest {

    @Test
    public void testTypeBranchCanExtendRootAssociationShape() {
        ClientTable table = ClientTable.$;
        AtomicReference<List<Client>> rows = new AtomicReference<>();
        jdbc(con -> rows.set(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(500L))
                        .select(
                                table.fetch(
                                        ClientFetcher.$
                                                .projects()
                                                .forType(
                                                        OrganizationFetcher.$.projects(
                                                                ClientProjectFetcher.$.name()
                                                        )
                                                )
                                )
                        )
                        .execute(con)
        ));
        assertEquals(1, rows.get().size());
        Client client = rows.get().get(0);
        assertEquals(Organization.class, ((ImmutableSpi) client).__type().getJavaClass());
        assertLoadState(client, "id", "projects");
        Organization organization = (Organization) client;
        assertEquals(1, organization.projects().size());
        assertLoadState(organization.projects().get(0), "id", "name");
        assertEquals("Cascade project", organization.projects().get(0).name());
    }
}
