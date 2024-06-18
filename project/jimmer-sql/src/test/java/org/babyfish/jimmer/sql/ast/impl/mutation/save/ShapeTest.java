package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.model.embedded.OrderItemDraft;
import org.babyfish.jimmer.sql.model.embedded.TransformDraft;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShapeTest extends AbstractQueryTest {

    @Test
    public void testScalar() {
        
        ImmutableSpi treeNode = (ImmutableSpi) TreeNodeDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setName("Root");
        });
        Shape shape = Shape.of((JSqlClientImplementor) getSqlClient(), treeNode);

        Assertions.assertEquals("[id, name]", shape.toString());

        Assertions.assertEquals(1L, shape.getGetters().get(0).get(treeNode));
        Assertions.assertEquals("Root", shape.getGetters().get(1).get(treeNode));

        Assertions.assertEquals("NODE_ID", shape.getGetters().get(0).columnName());
        Assertions.assertEquals("NAME", shape.getGetters().get(1).columnName());
    }

    @Test
    public void testReference() {

        ImmutableSpi treeNode = (ImmutableSpi) TreeNodeDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.setParentId(1L);
        });
        Shape shape = Shape.of((JSqlClientImplementor) getSqlClient(), treeNode);

        Assertions.assertEquals("[id, parent.id]", shape.toString());

        Assertions.assertEquals(2L, shape.getGetters().get(0).get(treeNode));
        Assertions.assertEquals(1L, shape.getGetters().get(1).get(treeNode));

        Assertions.assertEquals("NODE_ID", shape.getGetters().get(0).columnName());
        Assertions.assertEquals("PARENT_ID", shape.getGetters().get(1).columnName());
    }

    @Test
    public void testEmbeddedScalar() {
        ImmutableSpi transform = (ImmutableSpi) TransformDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.applySource(source -> {
                source.applyLeftTop(lt -> lt.setX(1));
                source.applyRightBottom(rb -> rb.setY(4));
            });
            draft.applyTarget(target -> {
                target.applyLeftTop(lt -> lt.setY(9));
                target.applyRightBottom(rb -> rb.setX(16));
            });
        });
        Shape shape = Shape.of((JSqlClientImplementor) getSqlClient(), transform);

        Assertions.assertEquals(
                "[id, source.leftTop.x, source.rightBottom.y, target.leftTop.y, target.rightBottom.x]",
                shape.toString()
        );

        Assertions.assertEquals(1L, shape.getGetters().get(0).get(transform));
        Assertions.assertEquals(1L, shape.getGetters().get(1).get(transform));
        Assertions.assertEquals(4L, shape.getGetters().get(2).get(transform));
        Assertions.assertEquals(9L, shape.getGetters().get(3).get(transform));
        Assertions.assertEquals(16L, shape.getGetters().get(4).get(transform));

        Assertions.assertEquals("ID", shape.getGetters().get(0).columnName());
        Assertions.assertEquals("`LEFT`", shape.getGetters().get(1).columnName());
        Assertions.assertEquals("BOTTOM", shape.getGetters().get(2).columnName());
        Assertions.assertEquals("TARGET_TOP", shape.getGetters().get(3).columnName());
        Assertions.assertEquals("TARGET_RIGHT", shape.getGetters().get(4).columnName());
    }

    @Test
    public void testEmbeddedReference() {
        ImmutableSpi orderGetter = (ImmutableSpi) OrderItemDraft.$.produce(draft -> {
            draft.applyId(id -> id.setA(1).setB(8).setC(27));
            draft.setOrderId(Objects.createOrderId(id -> id.setX("X-001").setY("Y-003")));
        });
        Shape shape = Shape.of((JSqlClientImplementor) getSqlClient(), orderGetter);

        Assertions.assertEquals(
                "[id.a, id.b, id.c, order.id.x, order.id.y]",
                shape.toString()
        );

        Assertions.assertEquals(1, shape.getGetters().get(0).get(orderGetter));
        Assertions.assertEquals(8, shape.getGetters().get(1).get(orderGetter));
        Assertions.assertEquals(27, shape.getGetters().get(2).get(orderGetter));
        Assertions.assertEquals("X-001", shape.getGetters().get(3).get(orderGetter));
        Assertions.assertEquals("Y-003", shape.getGetters().get(4).get(orderGetter));

        Assertions.assertEquals("ORDER_ITEM_A", shape.getGetters().get(0).columnName());
        Assertions.assertEquals("ORDER_ITEM_B", shape.getGetters().get(1).columnName());
        Assertions.assertEquals("ORDER_ITEM_C", shape.getGetters().get(2).columnName());
        Assertions.assertEquals("FK_ORDER_X", shape.getGetters().get(3).columnName());
        Assertions.assertEquals("FK_ORDER_Y", shape.getGetters().get(4).columnName());
    }
}
