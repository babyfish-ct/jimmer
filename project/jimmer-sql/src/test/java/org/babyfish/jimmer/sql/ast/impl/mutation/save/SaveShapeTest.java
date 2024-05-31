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

public class SaveShapeTest extends AbstractQueryTest {

    @Test
    public void testScalar() {

        MetadataStrategy strategy = ((JSqlClientImplementor)getSqlClient()).getMetadataStrategy();
        ImmutableSpi treeNode = (ImmutableSpi) TreeNodeDraft.$.produce(draft -> {
            draft.setId(1L);
            draft.setName("Root");
        });
        SaveShape shape = SaveShape.of(treeNode);

        Assertions.assertEquals("[id, name]", shape.toString());

        Assertions.assertEquals(1L, shape.getItmes().get(0).get(treeNode));
        Assertions.assertEquals("Root", shape.getItmes().get(1).get(treeNode));

        Assertions.assertEquals("NODE_ID", shape.getItmes().get(0).columnName(strategy));
        Assertions.assertEquals("NAME", shape.getItmes().get(1).columnName(strategy));
    }

    @Test
    public void testReference() {

        MetadataStrategy strategy = ((JSqlClientImplementor)getSqlClient()).getMetadataStrategy();
        ImmutableSpi treeNode = (ImmutableSpi) TreeNodeDraft.$.produce(draft -> {
            draft.setId(2L);
            draft.setParentId(1L);
        });
        SaveShape shape = SaveShape.of(treeNode);

        Assertions.assertEquals("[id, parent.id]", shape.toString());

        Assertions.assertEquals(2L, shape.getItmes().get(0).get(treeNode));
        Assertions.assertEquals(1L, shape.getItmes().get(1).get(treeNode));

        Assertions.assertEquals("NODE_ID", shape.getItmes().get(0).columnName(strategy));
        Assertions.assertEquals("PARENT_ID", shape.getItmes().get(1).columnName(strategy));
    }

    @Test
    public void testEmbeddedScalar() {
        MetadataStrategy strategy = ((JSqlClientImplementor)getSqlClient()).getMetadataStrategy();
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
        SaveShape shape = SaveShape.of(transform);

        Assertions.assertEquals(
                "[id, source.leftTop.x, source.rightBottom.y, target.leftTop.y, target.rightBottom.x]",
                shape.toString()
        );

        Assertions.assertEquals(1L, shape.getItmes().get(0).get(transform));
        Assertions.assertEquals(1L, shape.getItmes().get(1).get(transform));
        Assertions.assertEquals(4L, shape.getItmes().get(2).get(transform));
        Assertions.assertEquals(9L, shape.getItmes().get(3).get(transform));
        Assertions.assertEquals(16L, shape.getItmes().get(4).get(transform));

        Assertions.assertEquals("ID", shape.getItmes().get(0).columnName(strategy));
        Assertions.assertEquals("`LEFT`", shape.getItmes().get(1).columnName(strategy));
        Assertions.assertEquals("BOTTOM", shape.getItmes().get(2).columnName(strategy));
        Assertions.assertEquals("TARGET_TOP", shape.getItmes().get(3).columnName(strategy));
        Assertions.assertEquals("TARGET_RIGHT", shape.getItmes().get(4).columnName(strategy));
    }

    @Test
    public void testEmbeddedReference() {
        MetadataStrategy strategy = ((JSqlClientImplementor)getSqlClient()).getMetadataStrategy();
        ImmutableSpi orderItem = (ImmutableSpi) OrderItemDraft.$.produce(draft -> {
            draft.applyId(id -> id.setA(1).setB(8).setC(27));
            draft.setOrderId(Objects.createOrderId(id -> id.setX("X-001").setY("Y-003")));
        });
        SaveShape shape = SaveShape.of(orderItem);

        Assertions.assertEquals(
                "[id.a, id.b, id.c, order.id.x, order.id.y]",
                shape.toString()
        );

        Assertions.assertEquals(1, shape.getItmes().get(0).get(orderItem));
        Assertions.assertEquals(8, shape.getItmes().get(1).get(orderItem));
        Assertions.assertEquals(27, shape.getItmes().get(2).get(orderItem));
        Assertions.assertEquals("X-001", shape.getItmes().get(3).get(orderItem));
        Assertions.assertEquals("Y-003", shape.getItmes().get(4).get(orderItem));

        Assertions.assertEquals("ORDER_ITEM_A", shape.getItmes().get(0).columnName(strategy));
        Assertions.assertEquals("ORDER_ITEM_B", shape.getItmes().get(1).columnName(strategy));
        Assertions.assertEquals("ORDER_ITEM_C", shape.getItmes().get(2).columnName(strategy));
        Assertions.assertEquals("FK_ORDER_X", shape.getItmes().get(3).columnName(strategy));
        Assertions.assertEquals("FK_ORDER_Y", shape.getItmes().get(4).columnName(strategy));
    }
}
