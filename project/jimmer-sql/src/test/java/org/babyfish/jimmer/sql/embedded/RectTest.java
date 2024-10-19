package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.embedded.Rect;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.babyfish.jimmer.sql.model.embedded.dto.RectFlatView;
import org.babyfish.jimmer.sql.model.embedded.dto.RectView;
import org.junit.jupiter.api.Test;

public class RectTest extends AbstractQueryTest {

    @Test
    public void testRectView() {
        Rect rect = Immutables.createRect(draft -> {
            draft.applyLeftTop(leftTop -> leftTop.setX(1).setY(4));
            draft.applyRightBottom(rightBottom -> rightBottom.setX(9).setY(16));
        });
        RectView view = new RectView(rect);
        Tests.assertContentEquals(
                "RectView(" +
                        "--->leftTop=RectView.TargetOf_leftTop(x=1, y=4), " +
                        "--->rightBottom=RectView.TargetOf_rightBottom(x=9, y=16)" +
                        ")",
                view
        );
        Tests.assertContentEquals(
                "{\"leftTop\":{\"x\":1,\"y\":4},\"rightBottom\":{\"x\":9,\"y\":16}}",
                view.toImmutable()
        );
    }

    @Test
    public void testRectFlatView() {
        Rect rect = Immutables.createRect(draft -> {
            draft.applyLeftTop(leftTop -> leftTop.setX(1).setY(4));
            draft.applyRightBottom(rightBottom -> rightBottom.setX(9).setY(16));
        });
        RectFlatView view = new RectFlatView(rect);
        Tests.assertContentEquals(
                "RectFlatView(ltX=1, ltY=4, rbX=9, rbY=16)",
                view
        );
        Tests.assertContentEquals(
                "{\"leftTop\":{\"x\":1,\"y\":4},\"rightBottom\":{\"x\":9,\"y\":16}}",
                view.toImmutable()
        );
    }

    @Test
    public void testQueryRectView() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .select(
                                table.source().fetch(RectView.class),
                                table.target().fetch(RectFlatView.class)
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM tb_1_"
                    );
                    ctx.rows(list -> {
                        assertContentEquals(
                                "[" +
                                        "--->Tuple2(" +
                                        "--->--->_1=RectView(" +
                                        "--->--->--->leftTop=RectView.TargetOf_leftTop(x=100, y=120), " +
                                        "--->--->--->rightBottom=RectView.TargetOf_rightBottom(x=400, y=320)" +
                                        "--->--->), " +
                                        "--->--->_2=RectFlatView(ltX=800, ltY=600, rbX=1400, rbY=1000)" +
                                        "--->), " +
                                        "--->Tuple2(" +
                                        "--->--->_1=RectView(" +
                                        "--->--->--->leftTop=RectView.TargetOf_leftTop(x=150, y=170), " +
                                        "--->--->--->rightBottom=RectView.TargetOf_rightBottom(x=450, y=370)" +
                                        "--->--->), " +
                                        "--->--->_2=null" +
                                        "--->)" +
                                        "]",
                                list
                        );
                    });
                }
        );
    }
}
