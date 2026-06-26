package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoinedInheritanceQueryTest extends AbstractQueryTest {

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
}
