package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientImplicitCatchAllView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
}
