package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.hr.Department
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentSpecification
import org.babyfish.jimmer.sql.kt.model.hr.dto.DepartmentSpecification2
import kotlin.test.Test

class DepartmentSpecificationTest : AbstractQueryTest() {

    @Test
    fun testWithEmptySpecification() {
        val specification = DepartmentSpecification()
        executeAndExpect(
            sqlClient
                .createQuery(Department::class) {
                    where(specification)
                    select(table)
                }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_TIME " +
                    "from DEPARTMENT tb_1_ " +
                    "where tb_1_.DELETED_TIME is null"
            )
        }
    }

    @Test
    fun testWithNonEmptySpecification() {
        val specification = DepartmentSpecification(
            id = "3",
            employeeIds = listOf("4", "5")
        )
        executeAndExpect(
            sqlClient
                .createQuery(Department::class) {
                    where(specification)
                    select(table)
                }
        ) {
            sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_TIME " +
                    "from DEPARTMENT tb_1_ " +
                    "inner join EMPLOYEE tb_2_ on tb_1_.ID = tb_2_.DEPARTMENT_ID " +
                    "where tb_1_.ID = ? " +
                    "and tb_2_.ID in (?, ?) " +
                    "and tb_1_.DELETED_TIME is null"
            ).variables(3L, 4L, 5L)
        }
    }

    @Test
    fun testSpecification2() {
        val specification = DepartmentSpecification2(
            id = "3",
            employeeIds = listOf("4", "5"),
            employeeName = "Bob"
        )
        executeAndExpect(
            sqlClient.createQuery(Department::class) {
                where(specification)
                select(table)
            }
        ) {
            sql(
                """select tb_1_.ID, tb_1_.NAME, tb_1_.DELETED_TIME 
                    |from DEPARTMENT tb_1_ 
                    |where tb_1_.ID = ? 
                    |and exists(
                    |--->select 1 
                    |--->from EMPLOYEE tb_2_ 
                    |--->where tb_1_.ID = tb_2_.DEPARTMENT_ID 
                    |--->and tb_2_.ID in (?, ?) 
                    |--->and lower(tb_2_.NAME) like ? 
                    |--->and tb_2_.DELETED_UUID is null
                    |) and tb_1_.DELETED_TIME is null""".trimMargin()
            ).variables(3L, 4L, 5L, "%bob%")
        }
    }
}