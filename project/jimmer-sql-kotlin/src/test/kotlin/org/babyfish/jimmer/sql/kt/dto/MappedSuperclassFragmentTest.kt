package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.model.inheritance.dto.RoleWithMappedSuperclassFragmentView
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class MappedSuperclassFragmentTest {

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
