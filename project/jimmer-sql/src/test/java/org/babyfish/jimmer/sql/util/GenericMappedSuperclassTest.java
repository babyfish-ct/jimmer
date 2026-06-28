package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.model.generic.GenericTreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericMappedSuperclassTest {

    @Test
    public void testGenericTreeMetadata() {
        ImmutableType type = ImmutableType.get(GenericTreeNode.class);

        ImmutableProp parent = type.getProp("parent");
        assertEquals(GenericTreeNode.class, parent.getDeclaringType().getJavaClass());
        assertEquals(GenericTreeNode.class, parent.getReturnClass());
        assertEquals(GenericTreeNode.class, parent.getElementClass());
        assertEquals(GenericTreeNode.class.getName(), parent.getGenericType().getTypeName());
        assertSame(type, parent.getTargetType());
        assertTrue(parent.isReference(TargetLevel.ENTITY));

        ImmutableProp children = type.getProp("children");
        assertEquals(GenericTreeNode.class, children.getDeclaringType().getJavaClass());
        assertEquals(List.class, children.getReturnClass());
        assertEquals(GenericTreeNode.class, children.getElementClass());
        assertEquals(
                "java.util.List<" + GenericTreeNode.class.getName() + ">",
                children.getGenericType().getTypeName()
        );
        assertSame(type, children.getTargetType());
        assertEquals(ImmutablePropCategory.REFERENCE_LIST, children.getCategory());
        assertTrue(children.isReferenceList(TargetLevel.ENTITY));
    }
}
