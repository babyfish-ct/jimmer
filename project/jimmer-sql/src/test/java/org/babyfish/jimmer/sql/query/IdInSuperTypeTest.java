package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance2.AnimalTable;
import org.junit.jupiter.api.Test;

public class IdInSuperTypeTest extends AbstractQueryTest {

    @Test
    public void test() {
        AnimalTable animal = AnimalTable.$;
        executeAndExpect(
                getSqlClient().createQuery(animal).select(animal),
                ctx -> {
                    ctx.sql("select tb_1_.ID, tb_1_.NAME from ANIMAL tb_1_");
                    ctx.rows("[{\"id\":1,\"name\":\"Trigger\"},{\"id\":2,\"name\":\"Lion\"}]");
                }
        );
    }
}
