package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance4.OrganizationTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedInheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testSubtypeQueryWithRootAndSubtypeFields() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.name(), table.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME, tb_1__sub.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals("Globex", row.get_1());
                        assertEquals("GLOBEX-001", row.get_2());
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
                        .select(table.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME " +
                                    "from JOINED_CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, "\"Globex\"");
                }
        );
    }
}
