package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedCascadeInheritanceFetcherTest extends AbstractQueryTest {

    @Test
    public void testRootAssociationIsBatchedAcrossConcreteTypes() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(500L, 501L)))
                        .orderBy(table.id())
                        .select(
                                table.fetch(
                                        ClientFetcher.$.name().projects(
                                                ClientProjectFetcher.$.name()
                                        )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME " +
                                    "from JOINED_CASCADE_CLIENT tb_1_ " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables(500L, 501L);
                    ctx.statement(1).sql(
                            "select tb_1_.CLIENT_ID, tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_CASCADE_PROJECT tb_1_ " +
                                    "where tb_1_.CLIENT_ID in (?, ?)"
                    ).variables(500L, 501L);
                    ctx.rows(rows -> {
                        assertEquals(2, rows.size());
                        assertEquals(1, rows.get(0).projects().size());
                        assertEquals(0, rows.get(1).projects().size());
                    });
                }
        );
    }

    @Test
    public void testReturningRootAssociationIsBatchedAcrossConcreteTypes() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .getEntities()
                        .saveEntitiesCommand(Arrays.<Client>asList(
                                OrganizationDraft.$.produce(organization -> {
                                    organization.setId(500L);
                                    organization.setName("Cascade Globex+");
                                }),
                                PersonDraft.$.produce(person -> {
                                    person.setId(501L);
                                    person.setName("Cascade Alice+");
                                })
                        ))
                        .execute(
                                con,
                                ClientFetcher.$.allScalarFields().projects(
                                        ClientProjectFetcher.$.name()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select ID, CLIENT_TYPE, VERSION " +
                                    "from final table (" +
                                    "--->merge into JOINED_CASCADE_CLIENT tb_1_ " +
                                    "--->using(values(?, ?, ?, ?), (?, ?, ?, ?)) tb_2_(ID, NAME, VERSION, CLIENT_TYPE) " +
                                    "--->on tb_1_.ID = tb_2_.ID " +
                                    "--->when matched and tb_1_.CLIENT_TYPE = tb_2_.CLIENT_TYPE then update set NAME = tb_2_.NAME " +
                                    "--->when not matched then insert(ID, NAME, VERSION, CLIENT_TYPE) " +
                                    "--->values(tb_2_.ID, tb_2_.NAME, tb_2_.VERSION, tb_2_.CLIENT_TYPE)" +
                                    ")"
                    );
                    ctx.variables(
                            500L, "Cascade Globex+", 0, "ORG",
                            501L, "Cascade Alice+", 0, "Person"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE " +
                                    "from JOINED_CASCADE_CLIENT tb_1_ " +
                                    "where tb_1_.ID = any(?)"
                    );
                    ctx.statement(1).variables((Object) new Object[]{500L, 501L});
                    ctx.statement(2).sql(
                            "select tb_1_.CLIENT_ID, tb_1_.ID, tb_1_.NAME " +
                                    "from JOINED_CASCADE_PROJECT tb_1_ " +
                                    "where tb_1_.CLIENT_ID = any(?)"
                    );
                    ctx.statement(2).variables((Object) new Object[]{500L, 501L});
                    ctx.row(0, result -> {
                        assertEquals(2, result.getItems().size());
                        assertEquals(1, result.getItems().get(0).getModifiedEntity().projects().size());
                        assertEquals(0, result.getItems().get(1).getModifiedEntity().projects().size());
                    });
                }
        );
    }

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
