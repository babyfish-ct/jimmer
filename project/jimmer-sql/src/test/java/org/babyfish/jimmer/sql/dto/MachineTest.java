package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.dto.MachineViewFor539;
import org.junit.jupiter.api.Test;

public class MachineTest extends AbstractQueryTest {

    @Test
    public void testIssue539() {
        connectAndExpect(
                con -> {
                    return getSqlClient()
                            .getEntities()
                            .forConnection(con)
                            .findById(MachineViewFor539.class, 1L);
                },
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.HOST, tb_1_.PORT, tb_1_.SECONDARY_HOST " +
                                    "from MACHINE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(rows -> {
                        assertContentEquals(
                                "MachineViewFor539(" +
                                        "--->id=1, " +
                                        "--->hosts=[localhost], " +
                                        "--->location={\"host\":\"localhost\",\"port\":8080}" +
                                        ")",
                                rows.get(0)
                        );
                    });
                }
        );
    }
}
