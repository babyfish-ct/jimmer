package org.babyfish.jimmer.sql.time;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;


public class ZoneTest extends AbstractQueryTest {

    @Test
    public void test8() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(8)))
                        .getEntities()
                        .forConnection(con)
                        .findById(Administrator.class, 1L),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                    );
                    ctx.row(
                            0,
                            "{" +
                                    "--->\"name\":\"a_1\"," +
                                    "--->\"deleted\":false," +
                                    "--->\"createdTime\":\"2022-10-03 00:00:00\"," +
                                    "--->\"modifiedTime\":\"2022-10-03 00:10:00\"," +
                                    "--->\"id\":1" +
                                    "}"
                    );
                }
        );
    }

    @Test
    public void test0() {
        connectAndExpect(
                con -> getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(0)))
                        .getEntities()
                        .forConnection(con)
                        .findById(Administrator.class, 1L),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                    "from ADMINISTRATOR tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                    );
                    ctx.row(
                            0,
                            "{" +
                                    "--->\"name\":\"a_1\"," +
                                    "--->\"deleted\":false," +
                                    "--->\"createdTime\":\"2022-10-02 16:00:00\"," +
                                    "--->\"modifiedTime\":\"2022-10-02 16:10:00\"," +
                                    "--->\"id\":1" +
                                    "}"
                    );
                }
        );
    }
}
