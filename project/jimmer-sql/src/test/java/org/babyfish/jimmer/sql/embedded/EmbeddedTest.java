package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.embedded.*;
import org.junit.jupiter.api.Test;

public class EmbeddedTest extends AbstractQueryTest {

    private static final String ROWS = "[" +
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
            "]";

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
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindBySourceLeftTopX() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().leftTop().x().eq(100L))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.`LEFT` = ?"
                    ).variables(100L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindBySourceLeftTop() {
        TransformTable transform = TransformTable.$;
        Point point = PointDraft.$.produce(draft -> draft.setX(100).setY(120));
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().leftTop().eq(point))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where (tb_1_.`LEFT`, tb_1_.TOP) = (?, ?)"
                    ).variables(100L, 120L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testFindBySource() {
        TransformTable transform = TransformTable.$;
        Rect rect = RectDraft.$.produce(draft ->
                draft
                        .setLeftTop(leftTop -> leftTop.setX(100).setY(120))
                        .setRightBottom(leftTop -> leftTop.setX(400).setY(320))
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.source().eq(rect))
                        .select(transform),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where (tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM) = (?, ?, ?, ?)"
                    ).variables(100L, 120L, 400L, 320L);
                    ctx.rows(ROWS);
                }
        );
    }

    @Test
    public void testSelectLevel1() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source(),
                                transform.target()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                                    "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                                    "--->--->}," +
                                    "--->--->\"_3\":{" +
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
    public void testSelectLevel2() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source().leftTop(),
                                transform.source().rightBottom(),
                                transform.target().leftTop(),
                                transform.target().rightBottom()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":{\"x\":100,\"y\":120}," +
                                    "--->--->\"_3\":{\"x\":400,\"y\":320}," +
                                    "--->--->\"_4\":{\"x\":800,\"y\":600}," +
                                    "--->--->\"_5\":{\"x\":1400,\"y\":1000}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSelectLevel3() {
        TransformTable transform = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(transform)
                        .where(transform.id().eq(1L))
                        .select(
                                transform.id(),
                                transform.source().leftTop().x(),
                                transform.source().leftTop().y(),
                                transform.source().rightBottom().x(),
                                transform.source().rightBottom().y(),
                                transform.target().leftTop().x(),
                                transform.target().leftTop().y(),
                                transform.target().rightBottom().x(),
                                transform.target().rightBottom().y()
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM as tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":1," +
                                    "--->--->\"_2\":100," +
                                    "--->--->\"_3\":120," +
                                    "--->--->\"_4\":400," +
                                    "--->--->\"_5\":320," +
                                    "--->--->\"_6\":800," +
                                    "--->--->\"_7\":600," +
                                    "--->--->\"_8\":1400," +
                                    "--->--->\"_9\":1000" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
