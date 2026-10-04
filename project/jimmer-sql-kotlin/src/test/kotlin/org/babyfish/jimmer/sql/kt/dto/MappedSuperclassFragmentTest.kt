package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.model.generic.KGenericTreeNode
import org.babyfish.jimmer.sql.kt.model.generic.by
import org.babyfish.jimmer.sql.kt.model.generic.dto.InheritedTreeView
import org.babyfish.jimmer.sql.kt.model.inheritance.dto.RoleWithMappedSuperclassFragmentView
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MappedSuperclassFragmentTest {

    @Test
    fun testInheritedScalarRecursiveAssociationAndIdView() {
        val root = new(KGenericTreeNode::class).by {
            id = 1L
            name = "root"
            parent = null
        }
        val child = new(KGenericTreeNode::class).by {
            id = 2L
            name = "child"
            parent = root
        }

        val view = InheritedTreeView(child)
        assertEquals(2L, view.id)
        assertEquals("child", view.name)
        assertEquals(1L, view.parentId)
        assertEquals(InheritedTreeView(1L, "root", null, null), view.parent)

        val entity = view.toEntity()
        assertEquals(2L, entity.id)
        assertEquals("child", entity.name)
        assertEquals(1L, entity.parentId)
        assertEquals("root", view.parent!!.toEntity().name)
        assertNull(view.parent!!.toEntity().parent)
    }

    @Test
    fun testMappedSuperclassFragment() {
        val createdTime = LocalDateTime.of(2026, 7, 18, 12, 0)
        val modifiedTime = createdTime.plusHours(1)
        val view = RoleWithMappedSuperclassFragmentView(
            id = 1L,
            name = "admin",
            createdTime = createdTime,
            modifiedTime = modifiedTime
        )

        val role = view.toEntity()
        assertEquals(1L, role.id)
        assertEquals("admin", role.name)
        assertEquals(createdTime, role.createdTime)
        assertEquals(modifiedTime, role.modifiedTime)
    }
}
