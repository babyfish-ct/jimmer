package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.hr.Department
import org.babyfish.jimmer.sql.kt.model.hr.by
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentView
import kotlin.test.Test

class DepartmentViewTest {

    @Test
    fun testToEntity() {
        val view = DepartmentView(
            id = "3",
            employeeIds = listOf("4", "5")
        )
        assertContentEquals(
            """{"id":"3","employees":[{"id":4},{"id":5}]}""",
            view.toEntity()
        )
    }

    @Test
    fun testFromEntity() {
        val entity = new(Department::class).by {
            id = 3L
            employeeIds = listOf(4L, 5L)
        }
        assertContentEquals(
            "DepartmentView(id=3, employeeIds=[4, 5])",
            DepartmentView(entity)
        )
    }
}