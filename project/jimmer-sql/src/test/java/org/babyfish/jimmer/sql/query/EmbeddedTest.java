package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.Rect;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.junit.jupiter.api.Test;

public class EmbeddedTest extends AbstractQueryTest {

    @Test
    public void testFindById() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(1L);
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                                    "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                                    "--->--->}," +
                                    "--->--->\"target\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
                                    "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1000}" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testFindBySourceLeftAndTop() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .select(transform),
                ctx -> {
                    ctx.sql("");
                }
        );
    }
}
