package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.pg.PgDateTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class PgDataMutationTest extends AbstractMutationTest {

    @Test
    public void testInsert() {

        NativeDatabases.assumeNativeDatabase();

        LocalDate today = LocalDate.of(2024, 3, 29);
        Date now = new Date(2024, 3, 29, 13, 59, 59);
        PgDateTime dt = Immutables.createPgDateTime(draft -> {
           draft.setId(1L);
           draft.setDate(today);
           draft.setDateTime(now);
        });
        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> {
                    JSqlClient sqlClient8 = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(8)));
                    JSqlClient sqlClient0 = getSqlClient(it -> it.setZoneId(ZoneOffset.ofHours(0)));
                    int affectedRowCount = sqlClient8
                            .getEntities()
                            .forConnection(con)
                            .saveCommand(dt)
                            .setMode(SaveMode.INSERT_ONLY)
                            .execute()
                            .getTotalAffectedRowCount();
                    PgDateTime dt8 = sqlClient8
                            .getEntities()
                            .forConnection(con)
                            .findById(PgDateTime.class, 1L);
                    PgDateTime dt0 = sqlClient0
                            .getEntities()
                            .forConnection(con)
                            .findById(PgDateTime.class, 1L);
                    return new Tuple3<>(affectedRowCount, dt8, dt0);
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into PG_DATE_TIME(ID, dt, ts) values(?, ?, ?)");
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.dt, tb_1_.ts from PG_DATE_TIME tb_1_ where tb_1_.ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("select tb_1_.ID, tb_1_.dt, tb_1_.ts from PG_DATE_TIME tb_1_ where tb_1_.ID = ?");
                    });
                    ctx.value(
                            "Tuple3(" +
                                    "--->_1=1, " +
                                    "--->_2={" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"date\":\"2024-03-29\"," +
                                    "--->--->\"dateTime\":\"3924-04-29T05:59:59.000+00:00\"" +
                                    "--->}, " +
                                    "--->_3={" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"date\":\"2024-03-28\"," +
                                    "--->--->\"dateTime\":\"3924-04-29T05:59:59.000+00:00\"" +
                                    "--->}" +
                                    ")"
                    );
                }
        );
    }
}
