package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.hr.Employee
import org.babyfish.jimmer.sql.kt.model.hr.dto.EmployeeSpecificationForIssue735
import org.junit.Test

class EmployeeSpecificationTest : AbstractQueryTest() {

    @Test
    fun testParentEmpty() {
        val spec = EmployeeSpecificationForIssue735()
        executeAndExpect(
            sqlClient.createQuery(Employee::class) {
                where(spec)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DEPARTMENT_ID, tb_1_.DELETED_UUID 
                    |from EMPLOYEE tb_1_ 
                    |where tb_1_.DELETED_UUID is null""".trimMargin()
            )
            rows {  }
        }
    }

    @Test
    fun testParentIsNull() {
        val spec = EmployeeSpecificationForIssue735(isDepartmentNull = true)
        executeAndExpect(
            sqlClient.createQuery(Employee::class) {
                where(spec)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DEPARTMENT_ID, tb_1_.DELETED_UUID 
                    |from EMPLOYEE tb_1_ 
                    |where tb_1_.DEPARTMENT_ID is null and 
                    |tb_1_.DELETED_UUID is null""".trimMargin()
            )
        }
    }

    @Test
    fun testParentIsNullAndDepartmentName() {
        val spec = EmployeeSpecificationForIssue735(
            isDepartmentNull = true,
            departmentName = "ABC"
        )
        executeAndExpect(
            sqlClient.createQuery(Employee::class) {
                where(spec)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DEPARTMENT_ID, tb_1_.DELETED_UUID 
                    |from EMPLOYEE tb_1_ 
                    |inner join DEPARTMENT tb_2_ on tb_1_.DEPARTMENT_ID = tb_2_.ID 
                    |where tb_1_.DEPARTMENT_ID is null and 
                    |tb_2_.NAME = ? 
                    |and tb_1_.DELETED_UUID is null 
                    |and tb_2_.DELETED_TIME is null""".trimMargin()
            )
        }
    }
}