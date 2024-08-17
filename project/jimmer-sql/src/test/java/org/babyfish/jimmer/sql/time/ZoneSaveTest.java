package org.babyfish.jimmer.sql.time;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class ZoneSaveTest extends AbstractMutationTest {

    @Test()
    public void insertByZone0() {
        Administrator administrator = Objects.createAdministrator(draft -> {
            draft.setId(99L);
            draft.setName("SuperAdmin");
            draft.setCreatedTime(LocalDateTime.of(2024, 3, 28, 1, 0, 0));
            draft.setModifiedTime(LocalDateTime.of(2024, 3, 28, 13, 0, 0));
        });
        connectAndExpect(
                con -> {
                    JSqlClient sqlClient = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(0)));
                    int affectedRowCount = sqlClient
                            .getEntities()
                            .forConnection(con)
                            .saveCommand(administrator)
                            .setMode(SaveMode.INSERT_ONLY)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    Administrator admin1 = sqlClient
                            .getEntities()
                            .forConnection(con)
                            .findById(Administrator.class, 99L);
                    Administrator admin2 = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(8)))
                            .getEntities()
                            .forConnection(con)
                            .findById(Administrator.class, 99L);
                    return new Tuple3<>(affectedRowCount, admin1, admin2);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                        );
                    });
                    ctx.value(
                            "Tuple3(" +
                                    "--->_1=1, " +
                                    "--->_2={" +
                                    "--->--->\"name\":\"SuperAdmin\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2024-03-28 01:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2024-03-28 13:00:00\",\"id\":99" +
                                    "--->}, " +
                                    "--->_3={" +
                                    "--->--->\"name\":\"SuperAdmin\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2024-03-28 09:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2024-03-28 21:00:00\"," +
                                    "--->--->\"id\":99" +
                                    "--->}" +
                                    ")"
                    );
                }
        );
    }

    @Test()
    public void insertByZone8() {
        Administrator administrator = Objects.createAdministrator(draft -> {
            draft.setId(99L);
            draft.setName("SuperAdmin");
            draft.setCreatedTime(LocalDateTime.of(2024, 3, 28, 1, 0, 0));
            draft.setModifiedTime(LocalDateTime.of(2024, 3, 28, 13, 0, 0));
        });
        connectAndExpect(
                con -> {
                    JSqlClient sqlClient = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(8)));
                    int affectedRowCount = sqlClient
                            .getEntities()
                            .forConnection(con)
                            .saveCommand(administrator)
                            .setMode(SaveMode.INSERT_ONLY)
                            .execute(con)
                            .getTotalAffectedRowCount();
                    Administrator admin1 = sqlClient
                            .getEntities()
                            .forConnection(con)
                            .findById(Administrator.class, 99L);
                    Administrator admin2 = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(0)))
                            .getEntities()
                            .forConnection(con)
                            .findById(Administrator.class, 99L);
                    return new Tuple3<>(affectedRowCount, admin1, admin2);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(ID, NAME, DELETED, CREATED_TIME, MODIFIED_TIME) " +
                                        "values(?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED, tb_1_.CREATED_TIME, tb_1_.MODIFIED_TIME " +
                                        "from ADMINISTRATOR tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED <> ?"
                        );
                    });
                    ctx.value(
                            "Tuple3(" +
                                    "--->_1=1, " +
                                    "--->_2={" +
                                    "--->--->\"name\":\"SuperAdmin\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2024-03-28 01:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2024-03-28 13:00:00\",\"id\":99" +
                                    "--->}, " +
                                    "--->_3={" +
                                    "--->--->\"name\":\"SuperAdmin\"," +
                                    "--->--->\"deleted\":false," +
                                    "--->--->\"createdTime\":\"2024-03-27 17:00:00\"," +
                                    "--->--->\"modifiedTime\":\"2024-03-28 05:00:00\"," +
                                    "--->--->\"id\":99" +
                                    "--->}" +
                                    ")"
                    );
                }
        );
    }
}
