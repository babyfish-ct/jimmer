package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.PermissionFetcher;
import org.babyfish.jimmer.sql.model.inheritance.Role;
import org.babyfish.jimmer.sql.model.inheritance.RoleFetcher;
import org.babyfish.jimmer.sql.model.inheritance.RoleTable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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
                                    "--->{\"name\":\"r_1\",\"id\":100}," +
                                    "--->{\"name\":\"r_2\",\"id\":200}" +
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
                                    "--->--->--->{\"id\":1000},{" +
                                    "--->--->--->\"id\":2000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
                                    "--->},{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"id\":3000}," +
                                    "--->--->--->{\"id\":4000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
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
                                    "--->--->--->{\"name\":\"p_1\",\"id\":1000}," +
                                    "--->--->--->{\"name\":\"p_2\",\"id\":2000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":100" +
                                    "--->},{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"permissions\":[" +
                                    "--->--->--->{\"name\":\"p_3\",\"id\":3000}," +
                                    "--->--->--->{\"name\":\"p_4\",\"id\":4000}" +
                                    "--->--->]," +
                                    "--->--->\"id\":200" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testQueryByTime() {
        executeAndExpect(
                getSqlClient().createQuery(RoleTable.class, (q, role) -> {
                    q.where(
                            role.createdTime().between(
                                    LocalDateTime.of(2022, 10, 3, 0, 0, 0),
                                    LocalDateTime.of(2022, 10, 4, 0, 0, 0)
                            )
                    );
                    return q.select(role.fetch(RoleFetcher.$.allScalarFields().deleted(false)));
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ROLE as tb_1_ " +
                                    "where tb_1_.CREATED_TIME " +
                                    "between ? and ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"r_1\"," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"id\":100" +
                                    "--->},{" +
                                    "--->--->\"name\":\"r_2\"," +
                                    "--->--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->--->\"id\":200" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
