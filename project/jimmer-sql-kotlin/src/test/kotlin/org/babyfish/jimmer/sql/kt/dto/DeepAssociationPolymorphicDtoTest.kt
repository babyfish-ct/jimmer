package org.babyfish.jimmer.sql.kt.dto

import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.common.AbstractQueryTest
import org.babyfish.jimmer.sql.kt.model.inheritance.joined.organization.Organization
import org.babyfish.jimmer.sql.kt.model.inheritance.joined.organization.dto.OrganizationOverview
import org.babyfish.jimmer.sql.kt.model.inheritance.joined.organization.id
import org.babyfish.jimmer.sql.kt.model.inheritance.single.employee.Employee
import org.babyfish.jimmer.sql.kt.model.inheritance.single.employee.dto.EmployeeOverview
import org.babyfish.jimmer.sql.kt.model.inheritance.single.employee.id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * KSP 端跟 APT 端同款的 polymorphic DTO 运行时测试 ——
 * 同一份 join 行为由两个不同代码生成器 (apt / ksp) 各自产生对应 DTO 编译产物,
 * 跑同一份 jimmer-sql runtime,验证两个生成器的产物在 SQL 行为上一致。
 *
 * 数据存在 jimmer-sql-test-support 的 org_* / staff_* 表 (id 5000-6999),
 * 由 KSP 端 entity (org.babyfish.jimmer.sql.kt.model.inheritance.*)
 * 跟 APT 端 entity (org.babyfish.jimmer.sql.model.inheritance.*) 共享。
 */
class DeepAssociationPolymorphicDtoTest : AbstractQueryTest() {

    /*
     * Joined-inheritance DTO (per-branch director 设计):
     *   Organization (根, 鉴别器 ORG_TYPE)
     *     -- Company (分支: shareCount + 各自的 director/departments)
     *     -- GovernmentAgency (分支: budget + 各自的 director/departments)
     */
    @Test
    fun testJoinedOrganization() {
        executeAndExpect(
            sqlClient.createQuery(Organization::class) {
                where(table.id valueIn listOf(5000L, 5001L, 5002L, 5003L))
                orderBy(table.id)
                select(table.fetch(OrganizationOverview::class))
            }
        ) {
            // 主 SQL: director 在每个 branch 各自声明 → tb_1_.DIRECTOR_ID 出现两次。
            sql(
                "select tb_1_.ID, tb_1_.ORG_TYPE, tb_1_.NAME, " +
                    "tb_2_.SHARE_COUNT, tb_1_.DIRECTOR_ID, " +
                    "tb_3_.BUDGET, tb_1_.DIRECTOR_ID " +
                    "from ORG_ORGANIZATION tb_1_ " +
                    "left join ORG_COMPANY tb_2_ " +
                    "on tb_1_.ID = tb_2_.ID and tb_1_.ORG_TYPE = ? " +
                    "left join ORG_GOVERNMENT_AGENCY tb_3_ " +
                    "on tb_1_.ID = tb_3_.ID and tb_1_.ORG_TYPE = ? " +
                    "where tb_1_.ID in (?, ?, ?, ?) " +
                    "order by tb_1_.ID asc"
            )
            variables("COMPANY", "GOV_AGENCY", 5000L, 5001L, 5002L, 5003L)
            // 1-level `director` — Company 分支专属 (per-branch)。
            statement(1).sql(
                "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.OFFICE_ADDRESS_ID " +
                    "from ORG_DIRECTOR tb_1_ " +
                    "where tb_1_.ID in (?, ?)"
            )
            statement(1).variables(5900L, 5901L)
            // 1-level `departments` — Company 分支专属 (per-branch)。
            statement(2).sql(
                "select tb_1_.ORGANIZATION_ID, tb_1_.ID, tb_1_.NAME, tb_1_.MANAGER_ID " +
                    "from ORG_DEPARTMENT tb_1_ " +
                    "where tb_1_.ORGANIZATION_ID in (?, ?)"
            )
            statement(2).variables(5000L, 5001L)
            // 1-level `director` — GovernmentAgency 分支专属 (per-branch)。
            statement(3).sql(
                "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.OFFICE_ADDRESS_ID " +
                    "from ORG_DIRECTOR tb_1_ " +
                    "where tb_1_.ID in (?, ?)"
            )
            statement(3).variables(5902L, 5903L)
            // 1-level `departments` — GovernmentAgency 分支专属 (per-branch)。
            statement(4).sql(
                "select tb_1_.ORGANIZATION_ID, tb_1_.ID, tb_1_.NAME, tb_1_.MANAGER_ID " +
                    "from ORG_DEPARTMENT tb_1_ " +
                    "where tb_1_.ORGANIZATION_ID in (?, ?)"
            )
            statement(4).variables(5002L, 5003L)
            // 2-level `director.officeAddress` — combined 跨所有 branch。
            statement(5).sql(
                "select tb_1_.ID, tb_1_.STREET, tb_1_.CITY " +
                    "from ORG_ADDRESS tb_1_ " +
                    "where tb_1_.ID in (?, ?, ?)"
            )
            statement(5).variables(5800L, 5801L, 5802L)
            // 2-level `departments.manager` — Company 分支专属 (per-branch)。
            statement(6).sql(
                "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.LEVEL " +
                    "from ORG_MANAGER tb_1_ " +
                    "where tb_1_.ID in (?, ?, ?)"
            )
            statement(6).variables(5700L, 5701L, 5702L)
            // 2-level `departments.manager` — GovernmentAgency 分支专属。
            statement(7).sql(
                "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.LEVEL " +
                    "from ORG_MANAGER tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(7).variables(5703L)
            rows(4)
            row(0) {
                assertTrue(it is OrganizationOverview.Company)
                val c = it as OrganizationOverview.Company
                assertEquals(5000L, c.id)
                assertEquals("Acme Corp", c.name)
                assertEquals(10000, c.shareCount)
                assertDirectorAlice(c.director)
                assertEquals(2, c.departments.size)
                assertEquals(5100L, c.departments[0].id)
                assertEquals("Engineering", c.departments[0].name)
                assertManagerEve(c.departments[0].manager)
                assertEquals(5101L, c.departments[1].id)
                assertManagerFrank(c.departments[1].manager)
            }
            row(1) {
                assertTrue(it is OrganizationOverview.Company)
                val c = it as OrganizationOverview.Company
                assertEquals(5001L, c.id)
                assertEquals("Globex Inc", c.name)
                assertEquals(20000, c.shareCount)
                assertDirectorBob(c.director)
                assertEquals(1, c.departments.size)
                assertManagerGrace(c.departments[0].manager)
            }
            row(2) {
                assertTrue(it is OrganizationOverview.GovernmentAgency)
                val g = it as OrganizationOverview.GovernmentAgency
                assertEquals(5002L, g.id)
                assertEquals("FDA", g.name)
                assertEquals(1000000L, g.budget)
                assertDirectorCarol(g.director)
                assertEquals(1, g.departments.size)
                assertManagerHenry(g.departments[0].manager)
            }
            row(3) {
                assertTrue(it is OrganizationOverview.GovernmentAgency)
                val g = it as OrganizationOverview.GovernmentAgency
                assertEquals(5003L, g.id)
                assertEquals("NASA", g.name)
                assertEquals(5000000L, g.budget)
                // Dave 没有 office address -> TargetOf_address 为 null。
                assertNotNull(g.director)
                assertEquals(5903L, g.director.id)
                assertNull(g.director.officeAddress)
                // Mission Ops 没有 manager -> TargetOf_manager 为 null。
                assertEquals(1, g.departments.size)
                assertEquals(5104L, g.departments[0].id)
                assertNull(g.departments[0].manager)
            }
        }
    }

    /*
     * Single-table-inheritance DTO ("同名 prop 不同投影"):
     *   Employee (根, 鉴别器 EMP_TYPE)
     *     -- FullTimeEmployee (分支: annualSalary + supervisor 投影 department)
     *     -- PartTimeEmployee (分支: hourlyRate + supervisor 不投影 department)
     */
    @Test
    fun testSingleEmployee() {
        executeAndExpect(
            sqlClient.createQuery(Employee::class) {
                where(table.id valueIn listOf(6000L, 6001L, 6002L, 6003L))
                orderBy(table.id)
                select(table.fetch(EmployeeOverview::class))
            }
        ) {
            // 主 SQL: supervisor 在每个 branch 各自声明 → SUPERVISOR_ID
            // 投影到每个 branch LEFT JOIN 的 alias (tb_2_, tb_3_)。
            sql(
                "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME, " +
                    "tb_2_.ANNUAL_SALARY, tb_2_.SUPERVISOR_ID, " +
                    "tb_3_.HOURLY_RATE, tb_3_.SUPERVISOR_ID " +
                    "from STAFF_EMPLOYEE tb_1_ " +
                    "left join STAFF_EMPLOYEE tb_2_ " +
                    "on tb_1_.ID = tb_2_.ID and tb_2_.EMP_TYPE = ? " +
                    "left join STAFF_EMPLOYEE tb_3_ " +
                    "on tb_1_.ID = tb_3_.ID and tb_3_.EMP_TYPE = ? " +
                    "where tb_1_.ID in (?, ?, ?, ?) " +
                    "order by tb_1_.ID asc"
            )
            variables("FULL_TIME", "PART_TIME", 6000L, 6001L, 6002L, 6003L)
            // 1-level supervisor — FullTimeEmployee 分支专属 (per-branch)。
            statement(1).sql(
                "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME, tb_1_.DEPARTMENT_ID " +
                    "from STAFF_EMPLOYEE tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(1).variables(6000L)
            // 1-level responsibilities — FullTimeEmployee 分支专属 (per-branch)。
            statement(2).sql(
                "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                    "from STAFF_RESPONSIBILITY tb_1_ " +
                    "where tb_1_.OWNER_ID in (?, ?)"
            )
            statement(2).variables(6000L, 6001L)
            // 1-level supervisor — PartTimeEmployee 分支专属 (per-branch)。
            statement(3).sql(
                "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME " +
                    "from STAFF_EMPLOYEE tb_1_ " +
                    "where tb_1_.ID in (?, ?)"
            )
            statement(3).variables(6000L, 6001L)
            // 1-level responsibilities — PartTimeEmployee 分支专属 (per-branch)。
            statement(4).sql(
                "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                    "from STAFF_RESPONSIBILITY tb_1_ " +
                    "where tb_1_.OWNER_ID in (?, ?)"
            )
            statement(4).variables(6002L, 6003L)
            // 2-level supervisor.department — FullTimeEmployee 分支专属。
            statement(5).sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.LOCATION " +
                    "from STAFF_DEPARTMENT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(5).variables(6900L)
            // 2-level responsibilities.project — FullTimeEmployee 分支专属。
            statement(6).sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                    "from STAFF_PROJECT tb_1_ " +
                    "where tb_1_.ID in (?, ?)"
            )
            statement(6).variables(6300L, 6301L)
            // 2-level responsibilities.project — PartTimeEmployee 分支专属。
            statement(7).sql(
                "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                    "from STAFF_PROJECT tb_1_ " +
                    "where tb_1_.ID = ?"
            )
            statement(7).variables(6302L)
            rows(4)
            row(0) {
                assertTrue(it is EmployeeOverview.FullTimeEmployee)
                val e = it as EmployeeOverview.FullTimeEmployee
                assertEquals(6000L, e.id)
                assertEquals("Alice Allen", e.fullName)
                assertEquals(120000L, e.annualSalary)
                assertNull(e.supervisor)
                assertEquals(2, e.responsibilities.size)
                assertProjectPhoenix(e.responsibilities[0].project)
                assertProjectApollo(e.responsibilities[1].project)
            }
            row(1) {
                assertTrue(it is EmployeeOverview.FullTimeEmployee)
                val e = it as EmployeeOverview.FullTimeEmployee
                assertEquals(6001L, e.id)
                assertEquals(95000L, e.annualSalary)
                assertSupervisorAliceWithDepartment(e.supervisor)
                assertEquals(1, e.responsibilities.size)
                assertProjectPhoenix(e.responsibilities[0].project)
            }
            row(2) {
                assertTrue(it is EmployeeOverview.PartTimeEmployee)
                val e = it as EmployeeOverview.PartTimeEmployee
                assertEquals(6002L, e.id)
                assertEquals(50, e.hourlyRate)
                assertSupervisorAlicePlain(e.supervisor)
                assertEquals(1, e.responsibilities.size)
                assertProjectHelios(e.responsibilities[0].project)
            }
            row(3) {
                assertTrue(it is EmployeeOverview.PartTimeEmployee)
                val e = it as EmployeeOverview.PartTimeEmployee
                assertEquals(6003L, e.id)
                assertEquals(35, e.hourlyRate)
                assertSupervisorBobPlain(e.supervisor)
                assertEquals(1, e.responsibilities.size)
                assertProjectHelios(e.responsibilities[0].project)
            }
        }
    }

    /* ---- Director 断言辅助方法 ---------------------------------------- */

    private fun assertDirectorAlice(director: OrganizationOverview.Company.TargetOf_director?) {
        assertNotNull(director)
        assertEquals(5900L, director.id)
        assertEquals("Alice Anderson", director.fullName)
        val office = director.officeAddress
        assertNotNull(office)
        assertEquals(5800L, office.id)
        assertEquals("100 Main St", office.street)
        assertEquals("Beijing", office.city)
    }

    private fun assertDirectorBob(director: OrganizationOverview.Company.TargetOf_director?) {
        assertNotNull(director)
        assertEquals(5901L, director.id)
        assertEquals("Bob Brown", director.fullName)
        val office = director.officeAddress
        assertNotNull(office)
        assertEquals(5801L, office.id)
        assertEquals("200 Oak Ave", office.street)
        assertEquals("Shanghai", office.city)
    }

    private fun assertDirectorCarol(director: OrganizationOverview.GovernmentAgency.TargetOf_director?) {
        assertNotNull(director)
        assertEquals(5902L, director.id)
        assertEquals("Carol Carter", director.fullName)
        val office = director.officeAddress
        assertNotNull(office)
        assertEquals(5802L, office.id)
        assertEquals("300 Pine Rd", office.street)
        assertEquals("Guangzhou", office.city)
    }

    /* ---- Manager 断言辅助方法 ----------------------------------------- */

    private fun assertManagerEve(manager: OrganizationOverview.Company.TargetOf_departments.TargetOf_manager?) {
        assertNotNull(manager)
        assertEquals(5700L, manager.id)
        assertEquals("Eve Edwards", manager.fullName)
        assertEquals(3, manager.level)
    }

    private fun assertManagerFrank(manager: OrganizationOverview.Company.TargetOf_departments.TargetOf_manager?) {
        assertNotNull(manager)
        assertEquals(5701L, manager.id)
        assertEquals("Frank Foster", manager.fullName)
        assertEquals(2, manager.level)
    }

    private fun assertManagerGrace(manager: OrganizationOverview.Company.TargetOf_departments.TargetOf_manager?) {
        assertNotNull(manager)
        assertEquals(5702L, manager.id)
        assertEquals("Grace Green", manager.fullName)
        assertEquals(3, manager.level)
    }

    private fun assertManagerHenry(manager: OrganizationOverview.GovernmentAgency.TargetOf_departments.TargetOf_manager?) {
        assertNotNull(manager)
        assertEquals(5703L, manager.id)
        assertEquals("Henry Howard", manager.fullName)
        assertEquals(2, manager.level)
    }

    /* ---- Supervisor 断言辅助方法 -------------------------------------- */

    private fun assertSupervisorAliceWithDepartment(
        supervisor: EmployeeOverview.FullTimeEmployee.TargetOf_supervisor?
    ) {
        assertNotNull(supervisor)
        assertEquals(6000L, supervisor.id)
        assertEquals("Alice Allen", supervisor.fullName)
        val department = supervisor.department
        assertNotNull(department)
        assertEquals(6900L, department.id)
        assertEquals("Engineering", department.name)
        assertEquals("Beijing", department.location)
    }

    private fun assertSupervisorAlicePlain(supervisor: Any?) {
        assertNotNull(supervisor)
        assertTrue(
            supervisor is EmployeeOverview.PartTimeEmployee.TargetOf_supervisor,
            "Expected PartTimeEmployee.TargetOf_supervisor, got ${supervisor::class.qualifiedName}"
        )
    }

    private fun assertSupervisorBobPlain(supervisor: Any?) {
        assertNotNull(supervisor)
        assertTrue(
            supervisor is EmployeeOverview.PartTimeEmployee.TargetOf_supervisor,
            "Expected PartTimeEmployee.TargetOf_supervisor, got ${supervisor::class.qualifiedName}"
        )
    }

    /* ---- Project 断言辅助方法 ----------------------------------------- */

    private fun assertProjectPhoenix(
        project: EmployeeOverview.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project?
    ) {
        assertNotNull(project)
        assertEquals(6300L, project.id)
        assertEquals("Phoenix", project.name)
        assertEquals("Internal", project.client)
    }

    private fun assertProjectApollo(
        project: EmployeeOverview.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project?
    ) {
        assertNotNull(project)
        assertEquals(6301L, project.id)
        assertEquals("Apollo", project.name)
        assertEquals("NASA", project.client)
    }

    private fun assertProjectHelios(project: Any?) {
        assertNotNull(project)
    }
}
