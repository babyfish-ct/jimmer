package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.dto.ClientRuntimeView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JoinedTablePolymorphicDtoRuntimeTest extends AbstractQueryTest {

    @Test
    public void testExhaustiveBranchRouting() {
        ClientTable table = ClientTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(200L, 201L)))
                        .orderBy(table.id())
                        .select(table.fetch(ClientRuntimeView.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, " +
                                    "tb_2_.TAX_CODE, tb_3_.FIRST_NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "left join JOINED_ORGANIZATION tb_2_ " +
                                    "on tb_1_.ID = tb_2_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "left join JOINED_PERSON tb_3_ " +
                                    "on tb_1_.ID = tb_3_.ID and tb_1_.CLIENT_TYPE = ? " +
                                    "where tb_1_.ID in (?, ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("ORG", "Person", 200L, 201L);
                    ctx.row(0, row -> {
                        assertTrue(row instanceof ClientRuntimeView.Organization);
                        ClientRuntimeView.Organization organization = (ClientRuntimeView.Organization) row;
                        assertEquals(200L, organization.getId());
                        assertEquals("Globex", organization.getName());
                        assertEquals("GLOBEX-001", organization.getTaxCode());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof ClientRuntimeView.Person);
                        ClientRuntimeView.Person person = (ClientRuntimeView.Person) row;
                        assertEquals(201L, person.getId());
                        assertEquals("Alice", person.getName());
                        assertEquals("Alice", person.getFirstName());
                    });
                }
        );
    }
}
