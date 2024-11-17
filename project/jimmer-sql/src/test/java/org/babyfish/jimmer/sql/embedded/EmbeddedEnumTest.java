package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

public class EmbeddedEnumTest extends AbstractMutationTest {

    @Test
    public void testQuery() {
        UnitTable table = UnitTable.$;
        connectAndExpect(
                con ->  getSqlClient()
                        .createQuery(table)
                        .where(table.typeWrapper().type().eq(UnitType.RIFLEMAN))
                        .select(table)
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME, tb_1_.TYPE " +
                                        "from UNIT tb_1_ where tb_1_.TYPE = ?"
                        );
                        it.variables("RM");
                    });
                    ctx.value(
                            "[" +
                                    "--->{\"id\":3,\"name\":\"Carlisle\",\"typeWrapper\":{\"type\":\"RIFLEMAN\"}}, " +
                                    "--->{\"id\":4,\"name\":\"Garth\",\"typeWrapper\":{\"type\":\"RIFLEMAN\"}}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSave() {
        Unit sorceress = UnitDraft.$.produce(draft -> {
            draft.setName("Nadine");
            draft.typeWrapper(true).setType(UnitType.SORCERESS);
        });
        connectAndExpect(
                con -> getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .forConnection(con)
                        .save(sorceress)
                        .getModifiedEntity(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("merge into UNIT(NAME, TYPE) key(NAME) values(?, ?)");
                        it.variables("Nadine", "S");
                    });
                    ctx.value(
                            "{" +
                                    "--->\"id\":100," +
                                    "--->\"name\":\"Nadine\"," +
                                    "--->\"typeWrapper\":{\"type\":\"SORCERESS\"}" +
                                    "}"
                    );
                }
        );
    }
}
