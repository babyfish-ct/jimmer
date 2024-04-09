package org.babyfish.jimmer.sql.embedded;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.embedded.Rect;
import org.babyfish.jimmer.sql.model.embedded.dto.RectFlatView;
import org.babyfish.jimmer.sql.model.embedded.dto.RectView;
import org.junit.jupiter.api.Test;

public class RectTest {

    @Test
    public void testRectView() {
        Rect rect = Objects.createRect(draft -> {
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
        Rect rect = Objects.createRect(draft -> {
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
}
