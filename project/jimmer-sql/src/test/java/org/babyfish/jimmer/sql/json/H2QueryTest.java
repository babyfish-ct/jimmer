package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.json.Medicine;
import org.junit.jupiter.api.Test;

public class H2QueryTest extends AbstractQueryTest {

    @Test
    public void test() {
        connectAndExpect(con -> {
            return getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findById(Medicine.class, 1L);
        }, ctx -> {
            ctx.sql(
                    "select tb_1_.ID, tb_1_.TAGS " +
                            "from MEDICINE tb_1_ " +
                            "where tb_1_.ID = ?"
            );
            ctx.rows(
                    "[" +
                            "--->{" +
                            "--->--->\"id\":1," +
                            "--->--->\"tags\":[" +
                            "--->--->--->{\"name\":\"tag-1\",\"description\":\"tag-description-1\"}," +
                            "--->--->--->{\"name\":\"tag-2\",\"description\":\"tag-description-2\"}" +
                            "--->--->]" +
                            "--->}" +
                            "]"
            );
        });
    }
}
