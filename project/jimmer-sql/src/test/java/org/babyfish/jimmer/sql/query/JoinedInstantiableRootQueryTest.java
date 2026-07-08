package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Client;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.ClientFetcher;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable.Organization;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedInstantiableRootQueryTest extends AbstractQueryTest {

    @Test
    public void testRootFetcherMaterializesInstantiableRootWithoutUserDiscriminator() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(600L))
                        .select(table.fetch(ClientFetcher.$.name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_INST_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(600L);
                    ctx.row(0, row -> {
                        assertEquals(Client.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(600L, row.id());
                        assertEquals("Joined Root", row.name());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }

    @Test
    public void testRootFetcherMaterializesDerivedTypeWithoutUserDiscriminator() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(601L))
                        .select(table.fetch(ClientFetcher.$.name())),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_INST_CLIENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(601L);
                    ctx.row(0, row -> {
                        assertEquals(Organization.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(601L, row.id());
                        assertEquals("Joined Inst Org", row.name());
                        assertLoadState(row, "id", "name");
                    });
                }
        );
    }
}
