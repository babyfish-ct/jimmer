package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.PermissionFetcher;
import org.babyfish.jimmer.sql.model.inheritance.RoleFetcher;
import org.babyfish.jimmer.sql.model.inheritance.RoleTable;
import org.junit.jupiter.api.Test;

public class InheritanceQueryTest extends AbstractQueryTest {

    @Test
    public void testFetchLonely() {
        executeAndExpect(
                getSqlClient().createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .name()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{\"name\":\"r_1\",\"id\":1}," +
                                    "--->{\"name\":\"r_2\",\"id\":2}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchIdOnlyChildren() {
        executeAndExpect(
                getSqlClient().createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .name()
                                            .permissions()
                            )
                    );
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME from ROLE as tb_1_"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ROLE_ID, tb_1_.ID " +
                                    "from PERMISSION as tb_1_ " +
                                    "where tb_1_.ROLE_ID in (?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":1},{" +
                                    "--->--->--->\"id\":2}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":3}," +
                                    "--->--->--->{\"id\":4}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFetchChildren() {
        executeAndExpect(
                getSqlClient().createQuery(RoleTable.class, (q, role) -> {
                    return q.select(
                            role.fetch(
                                    RoleFetcher.$
                                            .name()
                                            .permissions(
                                                    PermissionFetcher.$.name()
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
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"name\":\"p_1\",\"id\":1}," +
                                    "--->--->--->{\"name\":\"p_2\",\"id\":2}" +
                                    "--->--->]," +
                                    "--->--->\"id\":1" +
                                    "--->},{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"name\":\"p_3\",\"id\":3}," +
                                    "--->--->--->{\"name\":\"p_4\",\"id\":4}" +
                                    "--->--->]," +
                                    "--->--->\"id\":2" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
