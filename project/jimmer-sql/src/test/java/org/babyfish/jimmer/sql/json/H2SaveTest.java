package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.model.json.Medicine;
import org.babyfish.jimmer.sql.model.json.MedicineDraft;
import org.babyfish.jimmer.sql.model.json.MedicineTable;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class H2SaveTest extends AbstractMutationTest {

    @Test
    public void testDML() {
        MedicineTable table = MedicineTable.$;
        executeAndExpectRowCount(
                getSqlClient(it -> it.setDialect(new H2Dialect()))
                        .createUpdate(table)
                        .set(
                                table.tags(),
                                Arrays.asList(
                                        new Medicine.Tag("Tag-1", "Description-1"),
                                        new Medicine.Tag("Tag-2", "Description-2")
                                )
                        )
                        .where(table.id().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update MEDICINE tb_1_ set TAGS = ? format json where tb_1_.ID = ?");
                        it.variables(
                                new DbLiteral.JsonWithSuffix(
                                        "[{\"name\":\"Tag-1\",\"description\":\"Description-1\"},{\"name\":\"Tag-2\",\"description\":\"Description-2\"}]",
                                        "format json"
                                ),
                                1L
                        );
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testSaveCommand() {
        connectAndExpect(con -> {
            int affectedCount = getSqlClient(it -> it.setDialect(new H2Dialect()))
                    .getEntities()
                    .saveCommand(
                            MedicineDraft.$.produce(draft -> {
                                draft.setId(1L);
                                draft.setTags(
                                        Arrays.asList(
                                                new Medicine.Tag("Tag-1", "Description-1"),
                                                new Medicine.Tag("Tag-2", "Description-2")
                                        )
                                );
                            })
                    ).setMode(SaveMode.UPDATE_ONLY)
                    .execute(con)
                    .getTotalAffectedRowCount();
            Medicine medicine = getSqlClient().getEntities().forConnection(con).findById(
                    Medicine.class,
                    1L
            );
            return new Tuple2<>(affectedCount, medicine);
        }, ctx -> {
            ctx.statement(it -> {
                it.sql("update MEDICINE set TAGS = ? format json where ID = ?");
            });
            ctx.statement(it -> {
                it.sql(
                        "select tb_1_.ID, tb_1_.TAGS " +
                                "from MEDICINE tb_1_ " +
                                "where tb_1_.ID = ?"
                );
            });
            ctx.value(
                    "Tuple2(" +
                            "--->_1=1, " +
                            "--->_2={" +
                            "--->--->\"id\":1," +
                            "--->--->\"tags\":[" +
                            "--->--->--->{\"name\":\"Tag-1\",\"description\":\"Description-1\"}," +
                            "--->--->--->{\"name\":\"Tag-2\",\"description\":\"Description-2\"}" +
                            "--->--->]" +
                            "--->}" +
                            ")"
            );
        });
    }
}
