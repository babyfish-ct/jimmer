package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.json.Medicine;
import org.babyfish.jimmer.sql.model.json.MedicineDraft;
import org.babyfish.jimmer.sql.model.json.MedicineTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class H2SaveTest extends AbstractMutationTest {

    @Test
    public void testDML() {
        MedicineTable table = MedicineTable.$;
        executeAndExpectRowCount(
                getSqlClient()
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
                        it.sql("update MEDICINE tb_1_ set TAGS = ? where tb_1_.ID = ?");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testSaveCommand() {
        executeAndExpectResult(
                getSqlClient().getEntities().saveCommand(
                        MedicineDraft.$.produce(draft -> {
                            draft.setId(1L);
                            draft.setTags(
                                    Arrays.asList(
                                            new Medicine.Tag("Tag-1", "Description-1"),
                                            new Medicine.Tag("Tag-2", "Description-2")
                                    )
                            );
                        })
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update MEDICINE set TAGS = ? where ID = ?");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{" +
                                        "\"id\":1," +
                                        "\"tags\":[" +
                                        "--->{\"name\":\"Tag-1\",\"description\":\"Description-1\"}," +
                                        "--->{\"name\":\"Tag-2\",\"description\":\"Description-2\"}" +
                                        "]" +
                                        "}"
                        );
                    });
                }
        );
    }
}
