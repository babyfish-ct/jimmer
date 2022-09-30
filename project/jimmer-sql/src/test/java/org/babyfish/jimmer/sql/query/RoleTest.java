package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.permission.PermissionFetcher;
import org.babyfish.jimmer.sql.model.permission.RoleFetcher;
import org.babyfish.jimmer.sql.model.permission.RoleTable;
import org.junit.jupiter.api.Test;

public class RoleTest extends AbstractQueryTest {

    @Test
    public void testFetchByInheritance() {
        executeAndExpect(
                getSqlClient().createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .allScalarFields()
                                            .permissions(
                                                    PermissionFetcher.$.allScalarFields()
                                            )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_");
                    ctx.statement(1).sql(
                            "select tb_1_.ROLE_ID, tb_1_.ID, tb_1_.NAME " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.ROLE_ID in (?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":1,\"name\":\"p_1\"}," +
                                    "--->--->--->{\"id\":2,\"name\":\"p_2\"}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1," +
                                    "--->--->\"name\":\"r_1\"" +
                                    "--->},{" +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":3,\"name\":\"p_3\"}," +
                                    "--->--->--->{\"id\":4,\"name\":\"p_4\"}]," +
                                    "--->--->\"id\":2," +
                                    "--->--->\"name\":\"r_2\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
