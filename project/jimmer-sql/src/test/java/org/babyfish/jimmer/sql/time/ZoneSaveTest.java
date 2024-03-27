package org.babyfish.jimmer.sql.time;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ZoneSaveTest extends AbstractMutationTest {

    @Test()
    public void insert8() {

        Assumptions.abort("Not ready");

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
                    Administrator newAdministrator = sqlClient
                            .getEntities()
                            .forConnection(con)
                            .findById(Administrator.class, 99L);
                    return new Tuple2<>(affectedRowCount, newAdministrator);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into ADMINISTRATOR(NAME, DELETED, CREATED_TIME, MODIFIED_TIME, ID) " +
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
                    ctx.value(
                            ""
                    );
                }
        );
    }
}
