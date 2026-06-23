package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance3.OrganizationTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SingleTableInheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testSubtypeQueryWithRootAndSubtypeFields() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(table.name(), table.taxCode()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NAME, tb_1_.TAX_CODE " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ?"
                    ).variables("ORG");
                    ctx.row(0, row -> {
                        assertEquals("Acme", row.get_1());
                        assertEquals("ACME-001", row.get_2());
                    });
                }
        );
    }
}
