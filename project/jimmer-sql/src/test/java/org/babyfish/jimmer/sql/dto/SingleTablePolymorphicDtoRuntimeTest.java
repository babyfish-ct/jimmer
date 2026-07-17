package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientProject;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientProjectTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Organization;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitCatchAllView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientProjectWithClientView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientProjectWithReusableClientView;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientProjectWithReusableParticipantsView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SingleTablePolymorphicDtoRuntimeTest extends AbstractQueryTest {

    @Test
    public void testImplicitDefaultBranchRouting() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(100L, 101L)))
                        .orderBy(table.id())
                        .select(table.fetch(ClientImplicitCatchAllView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", 100L, 101L);
                    ctx.row(0, row -> {
                        assertTrue(row instanceof ClientImplicitCatchAllView.Organization);
                        ClientImplicitCatchAllView.Organization organization =
                                (ClientImplicitCatchAllView.Organization) row;
                        assertEquals(100L, organization.getId());
                        assertEquals("Acme", organization.getName());
                        assertEquals("ACME-001", organization.getTaxCode());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof ClientImplicitCatchAllView.Default);
                        ClientImplicitCatchAllView.Default defaultBranch =
                                (ClientImplicitCatchAllView.Default) row;
                        assertEquals(101L, defaultBranch.getId());
                        assertEquals("Bob", defaultBranch.getName());
                    });
                }
        );
    }

    @Test
    public void testNestedPolymorphicAssociationRouting() {
        ClientProjectTable table = ClientProjectTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1000L))
                        .select(table.fetch(ClientProjectWithClientView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_ID " +
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1000L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID = ?"
                    ).variables("ORG", 100L);
                    ctx.row(0, row -> {
                        assertEquals(1000L, row.getId());
                        assertEquals("Single root project", row.getName());
                        assertTrue(row.getClient() instanceof ClientProjectWithClientView.TargetOf_client.Organization);
                        ClientProjectWithClientView.TargetOf_client.Organization organization =
                                (ClientProjectWithClientView.TargetOf_client.Organization) row.getClient();
                        assertEquals(100L, organization.getId());
                        assertEquals("Acme", organization.getName());
                        assertEquals("ACME-001", organization.getTaxCode());
                    });
                }
        );
    }

    @Test
    public void testReusablePolymorphicAssociationRouting() {
        ClientProjectTable table = ClientProjectTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1000L))
                        .select(table.fetch(ClientProjectWithReusableClientView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT_ID " +
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1000L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID = ?"
                    ).variables("ORG", 100L);
                    ctx.row(0, row -> {
                        assertTrue(row.getClient() instanceof ClientImplicitCatchAllView.Organization);
                        ClientImplicitCatchAllView.Organization organization =
                                (ClientImplicitCatchAllView.Organization) row.getClient();
                        assertEquals("ACME-001", organization.getTaxCode());
                    });
                }
        );
    }

    @Test
    public void testReusablePolymorphicListAssociationRouting() {
        ClientProjectTable table = ClientProjectTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1000L))
                        .select(table.fetch(ClientProjectWithReusableParticipantsView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from SINGLE_CLIENT_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1000L);
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_2_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "inner join SINGLE_CLIENT_PROJECT_PARTICIPANT_MAPPING tb_3_ " +
                                    "on tb_1_.ID = tb_3_.CLIENT_ID " +
                                    "left join CLIENT tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_3_.PROJECT_ID = ?"
                    ).variables("ORG", 1000L);
                    ctx.row(0, row -> {
                        List<ClientImplicitCatchAllView> participants = row.getParticipants();
                        assertEquals(2, participants.size());
                        assertTrue(participants.get(0) instanceof ClientImplicitCatchAllView.Organization);
                        assertTrue(participants.get(1) instanceof ClientImplicitCatchAllView.Default);
                        assertEquals(
                                "ACME-001",
                                ((ClientImplicitCatchAllView.Organization) participants.get(0)).getTaxCode()
                        );

                        ClientProject project = row.toImmutable();
                        assertEquals(2, project.participants().size());
                        assertTrue(project.participants().get(0) instanceof Organization);
                    });
                }
        );
    }
}
