package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.meta.ImmutablePropCategory
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.kt.model.generic.KGenericTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GenericMappedSuperclassTest {

    @Test
    fun testGenericTreeMetadata() {
        val type = ImmutableType.get(KGenericTreeNode::class.java)

        val parent = type.getProp("parent")
        assertEquals(KGenericTreeNode::class.java, parent.declaringType.javaClass)
        assertEquals(KGenericTreeNode::class.java, parent.returnClass)
        assertEquals(KGenericTreeNode::class.java, parent.elementClass)
        assertEquals(KGenericTreeNode::class.java.name, parent.genericType.typeName)
        assertSame(type, parent.targetType)
        assertTrue(parent.isReference(TargetLevel.ENTITY))

        val children = type.getProp("children")
        assertEquals(KGenericTreeNode::class.java, children.declaringType.javaClass)
        assertEquals(List::class.java, children.returnClass)
        assertEquals(KGenericTreeNode::class.java, children.elementClass)
        assertEquals(
            "java.util.List<${KGenericTreeNode::class.java.name}>",
            children.genericType.typeName
        )
        assertSame(type, children.targetType)
        assertEquals(ImmutablePropCategory.REFERENCE_LIST, children.category)
        assertTrue(children.isReferenceList(TargetLevel.ENTITY))
    }
}
