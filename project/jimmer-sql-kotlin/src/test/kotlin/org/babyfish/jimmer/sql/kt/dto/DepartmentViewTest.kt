package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.kt.common.assertContentEquals
import org.babyfish.jimmer.sql.kt.model.hr.Department
import org.babyfish.jimmer.sql.kt.model.hr.addBy
import org.babyfish.jimmer.sql.kt.model.hr.by
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentCompositeView
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentIdFunView
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
            """{"id":"3","employees":[{"id":"4"},{"id":"5"}]}""",
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

    @Test
    fun testToEntityByIdFun() {
        val view = DepartmentIdFunView(
            id = "3",
            employeeIds = listOf("4", "5")
        )
        assertContentEquals(
            """{"id":"3","employees":[{"id":"4"},{"id":"5"}]}""",
            view.toEntity()
        )
    }

    @Test
    fun testFromEntityByIdFun() {
        val entity = new(Department::class).by {
            id = 3L
            employeeIds = listOf(4L, 5L)
        }
        assertContentEquals(
            "DepartmentIdFunView(id=3, employeeIds=[4, 5])",
            DepartmentIdFunView(entity)
        )
    }

    @Test
    fun testToEntityByComposite() {
        val view = DepartmentCompositeView(
            id = "3",
            employees = listOf(
                DepartmentCompositeView.TargetOf_employees(
                    id = "4",
                    name = "Jim"
                ),
                DepartmentCompositeView.TargetOf_employees(
                    id = "5",
                    name = "Kate"
                )
            )
        )
        assertContentEquals(
            """{
                |--->"id":"3",
                |--->"employees":[
                |--->--->{"id":"4","name":"Jim"},
                |--->--->{"id":"5","name":"Kate"}
                |--->]
                |}""".trimMargin(),
            view.toEntity()
        )
    }

    @Test
    fun testFromEntityByComposite() {
        val entity = new(Department::class).by {
            id = 3L
            employees().addBy {
                id = 4L
                name = "Jim"
            }
            employees().addBy {
                id = 4L
                name = "Kate"
            }
        }
        assertContentEquals(
            """DepartmentCompositeView(
                |--->id=3, 
                |--->employees=[
                |--->--->TargetOf_employees(id=4, name=Jim), 
                |--->--->TargetOf_employees(id=4, name=Kate)
                |--->]
                |)""".trimMargin(),
            DepartmentCompositeView(entity)
        )
    }
}