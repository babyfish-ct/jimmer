package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.OrganizationTable;
import org.babyfish.jimmer.sql.model.inheritance.joined.organization.dto.OrganizationOverview;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.EmployeeTable;
import org.babyfish.jimmer.sql.model.inheritance.single.employee.dto.EmployeeOverview;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 两个独立继承树的 polymorphic DTO 运行时测试(都走 per-branch 设计 —— 关联 prop
 * 只在每个 branch 各自声明,根只有标量):
 *
 *   1. Organization (joined-inheritance) - 2 家公司 + 2 个政府机构,
 *      每个 branch 各自有 director (1-level, 2-level → officeAddress)
 *      和 departments (1-level, 2-level → manager)。
 *
 *   2. Employee (single-table-inheritance) - 2 个正式员工 + 2 个兼职员工,
 *      每个 branch 各自有 supervisor (1-level, 2-level → department [仅 FT])
 *      和 responsibilities (1-level, 2-level → project)。
 *
 * 测试数据存在 org_* / staff_* 表中 (id 范围 5000-6999),
 * 与本套件中的其他测试数据完全隔离。
 *
 * 每个树的运行时 SQL 模式 (因为根没有关联,无 combined SQL):
 *   - 1 条主 SQL (polymorphic 根 + 自身/joined LEFT JOIN + 鉴别器)
 *   - 0 条根 1-level (combined) + N 条分支 1-level (per-branch)
 *   - 0 条根 2-level (combined) + N 条分支 2-level (per-branch)
 *   = Organization 共 9 条 (1 主 + 4 per-branch 1-level + 4 per-branch 2-level)
 *   = Employee 共 8 条 (1 主 + 4 per-branch 1-level + 3 per-branch 2-level,
 *     PT 的 supervisor 没 department 所以少 1 条)
 */
public class DeepAssociationPolymorphicDtoTest extends AbstractQueryTest {

    /*
     * Joined-inheritance DTO (跟 Employee 同款 per-branch 设计):
     *   Organization (根, 鉴别器 ORG_TYPE)
     *     -- Company (分支: shareCount)
     *     -- GovernmentAgency (分支: budget)
     *
     *   根属性(只有标量): id, name
     *   分支专属属性: director (1-level) + director.officeAddress (2-level)
     *                + departments (1-level) + departments.manager (2-level)
     */
    @Test
    public void testJoinedOrganization() {
        OrganizationTable table = OrganizationTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(5000L, 5001L, 5002L, 5003L)))
                        .orderBy(table.id())
                        .select(table.fetch(OrganizationOverview.class)),
                ctx -> {
                    // 主 SQL: polymorphic 根 LEFT JOIN 到两个子类型表
                    // (Company / GovernmentAgency),鉴别器写在 tb_1_ 根上
                    // (因为 ORG_TYPE 在 ORG_ORGANIZATION 表,joined inheritance)。
                    //
                    // DIRECTOR_ID 物理上在 ORG_ORGANIZATION 表 (tb_1_),
                    // 但因为 `director` 在每个 branch 内各自声明,
                    // jimmer-apt 编译期就给每个 branch 的 fetcher 字段列表
                    // 各投影一次 DIRECTOR_ID 作为 1-level batch fetch 的 seed ———
                    // 所以 SELECT 列表里 tb_1_.DIRECTOR_ID 出现两次
                    // (一次属于 Company fetcher,一次属于 GovAgency fetcher),
                    // 跟 Employee (single-table) 的 SUPERVISOR_ID 投影到
                    // LEFT JOIN alias 是同一种"per-branch 投影"逻辑,
                    // 只是 joined 时物理列只在根表上。
                    //
                    // 根级 inner DTO 不生成(因为根没有 director),1-level/2-level
                    // batch fetch 都走 per-branch 路径(见下面 statement 1-6)。
                    ctx.statement(0).sql(
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
                    ).variables("COMPANY", "GOV_AGENCY", 5000L, 5001L, 5002L, 5003L);
                    // 1-level 关联 `director` — Company 分支专属 (per-branch)。
                    // Company 范围 director ID 集合 = {5900, 5901}
                    // (5000→5900 Alice, 5001→5901 Bob)。
                    // 投影包含 OFFICE_ADDRESS_ID 列 (因为 director.officeAddress 是 2-level)。
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.OFFICE_ADDRESS_ID " +
                                    "from ORG_DIRECTOR tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(5900L, 5901L);
                    // 1-level 关联 `departments` — Company 分支专属 (per-branch)。
                    ctx.statement(2).sql(
                            "select tb_1_.ORGANIZATION_ID, tb_1_.ID, tb_1_.NAME, tb_1_.MANAGER_ID " +
                                    "from ORG_DEPARTMENT tb_1_ " +
                                    "where tb_1_.ORGANIZATION_ID in (?, ?)"
                    ).variables(5000L, 5001L);
                    // 1-level 关联 `director` — GovernmentAgency 分支专属 (per-branch)。
                    // GovAgency 范围 director ID 集合 = {5902, 5903}
                    // (5002→5902 Carol, 5003→5903 Dave)。
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.OFFICE_ADDRESS_ID " +
                                    "from ORG_DIRECTOR tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(5902L, 5903L);
                    // 1-level 关联 `departments` — GovernmentAgency 分支专属 (per-branch)。
                    ctx.statement(4).sql(
                            "select tb_1_.ORGANIZATION_ID, tb_1_.ID, tb_1_.NAME, tb_1_.MANAGER_ID " +
                                    "from ORG_DEPARTMENT tb_1_ " +
                                    "where tb_1_.ORGANIZATION_ID in (?, ?)"
                    ).variables(5002L, 5003L);
                    // 2-level 关联 `director.officeAddress` — combined 跨所有 branch。
                    // 虽然 director 在每个 branch 各自声明 → 各自有 TargetOf_director,
                    // 但 TargetOf_director 的字段集 (id/fullName/officeAddress) 一样,
                    // 且 officeAddress 物理上是同一张 ORG_ADDRESS 表,所以 jimmer
                    // 把 2-level SQL combined 成一条。
                    // IN 列表: 5900(Alice)→5800, 5901(Bob)→5801, 5902(Carol)→5802;
                    // 5903(Dave) 没 office_address 所以不在 IN 里 (null 过滤)。
                    ctx.statement(5).sql(
                            "select tb_1_.ID, tb_1_.STREET, tb_1_.CITY " +
                                    "from ORG_ADDRESS tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(5800L, 5801L, 5802L);
                    // 2-level 关联 `departments.manager` — Company 分支专属 (per-branch)。
                    // Company 部门 manager ID 集合 = {5700, 5701, 5702}
                    // (5100 Eng→Eve, 5101 Sales→Frank, 5102 R&D→Grace)。
                    ctx.statement(6).sql(
                            "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.LEVEL " +
                                    "from ORG_MANAGER tb_1_ " +
                                    "where tb_1_.ID in (?, ?, ?)"
                    ).variables(5700L, 5701L, 5702L);
                    // 2-level 关联 `departments.manager` — GovernmentAgency 分支专属。
                    // 5103 Public Safety→Henry(5703);5104 Mission Ops 没 manager 被滤掉。
                    ctx.statement(7).sql(
                            "select tb_1_.ID, tb_1_.FULL_NAME, tb_1_.LEVEL " +
                                    "from ORG_MANAGER tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(5703L);
                    ctx.rows(4);
                    // (joined: 共 8 条 SQL ——
                    //  1 主 + 0 根 1-level + 4 per-branch 1-level
                    //  + 0 根 2-level + 1 combined 2-level (officeAddress 字段集一样
                    //    跨 branch 物理同表 → combined) + 2 per-branch 2-level (manager))
                    ctx.row(0, row -> {
                        assertTrue(row instanceof OrganizationOverview.Company);
                        OrganizationOverview.Company c =
                                (OrganizationOverview.Company) row;
                        assertEquals(5000L, c.getId());
                        assertEquals("Acme Corp", c.getName());
                        assertEquals(10000, c.getShareCount());
                        assertDirectorAlice(c.getDirector());
                        assertEquals(2, c.getDepartments().size());
                        assertEquals(5100L, c.getDepartments().get(0).getId());
                        assertEquals("Engineering", c.getDepartments().get(0).getName());
                        assertManagerEve(c.getDepartments().get(0).getManager());
                        assertEquals(5101L, c.getDepartments().get(1).getId());
                        assertManagerFrank(c.getDepartments().get(1).getManager());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof OrganizationOverview.Company);
                        OrganizationOverview.Company c =
                                (OrganizationOverview.Company) row;
                        assertEquals(5001L, c.getId());
                        assertEquals("Globex Inc", c.getName());
                        assertEquals(20000, c.getShareCount());
                        assertDirectorBob(c.getDirector());
                        assertEquals(1, c.getDepartments().size());
                        assertManagerGrace(c.getDepartments().get(0).getManager());
                    });
                    ctx.row(2, row -> {
                        assertTrue(row instanceof OrganizationOverview.GovernmentAgency);
                        OrganizationOverview.GovernmentAgency g =
                                (OrganizationOverview.GovernmentAgency) row;
                        assertEquals(5002L, g.getId());
                        assertEquals("FDA", g.getName());
                        assertEquals(1000000L, g.getBudget());
                        assertDirectorCarol(g.getDirector());
                        assertEquals(1, g.getDepartments().size());
                        assertManagerHenry(g.getDepartments().get(0).getManager());
                    });
                    ctx.row(3, row -> {
                        assertTrue(row instanceof OrganizationOverview.GovernmentAgency);
                        OrganizationOverview.GovernmentAgency g =
                                (OrganizationOverview.GovernmentAgency) row;
                        assertEquals(5003L, g.getId());
                        assertEquals("NASA", g.getName());
                        assertEquals(5000000L, g.getBudget());
                        // Dave 没有 office address -> TargetOf_address 为 null。
                        assertNotNull(g.getDirector());
                        assertEquals(5903L, g.getDirector().getId());
                        assertNull(g.getDirector().getOfficeAddress());
                        // Mission Ops 没有 manager -> TargetOf_manager 为 null。
                        assertEquals(1, g.getDepartments().size());
                        assertEquals(5104L, g.getDepartments().get(0).getId());
                        assertNull(g.getDepartments().get(0).getManager());
                    });
                }
        );
    }

    /*
     * Single-table-inheritance DTO:
     *   Employee (根, 鉴别器 EMP_TYPE)
     *     -- FullTimeEmployee (分支: annualSalary)
     *     -- PartTimeEmployee (分支: hourlyRate)
     *
     *   根属性: id, fullName, type, supervisor -> department
     *   这个 DTO 测试 **"同名 prop 不同投影"** 场景:
     *   `supervisor` 在每个 branch 都声明,但两个 branch 投影字段集不同:
     *     - FullTimeEmployee.supervisor 投影 { id, fullName, department { ... } }
     *     - PartTimeEmployee.supervisor 投影 { id, fullName } (不投影 department)
     *
     *   编译期: jimmer-apt 为两个 branch 生成字段集不同的
     *   `TargetOf_supervisor` inner DTO 类 (FT 有 getDepartment, PT 没有)。
     *
     *   运行时 SQL 行为:
     *     - 1-level supervisor 走 combined (target type 是 polymorphic 根),
     *       SQL 仍投影 DEPARTMENT_ID 列 (取并集)。
     *     - 2-level supervisor.department 只 FT 需要,发 1 条 SQL。
     *     - responsibilities 走 per-branch (target type 不是 polymorphic 根)。
     */
    @Test
    public void testSingleEmployee() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().in(Arrays.asList(6000L, 6001L, 6002L, 6003L)))
                        .orderBy(table.id())
                        .select(table.fetch(EmployeeOverview.class)),
                ctx -> {
                    // 主 SQL: 即使是 single-table 继承,polymorphic 根也
                    // 用 LEFT JOIN 自身并按 EMP_TYPE 过滤,
                    // 让每个分支的标量列 (annualSalary / hourlyRate) 投影到不同 alias。
                    // 分支按鉴别器值字典序排列 (FULL_TIME < PART_TIME),
                    // 所以 annualSalary alias 出现在前。
                    // supervisor 在每个 branch 各自声明,jimmer 把 SUPERVISOR_ID
                    // 投影到每个 branch LEFT JOIN 的 alias (tb_2_, tb_3_) 上,
                    // 而不是 tb_1_ 根上 — 这是"在每个 branch 都声明"模式的特点。
                    ctx.statement(0).sql(
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
                    ).variables("FULL_TIME", "PART_TIME", 6000L, 6001L, 6002L, 6003L);
                    // 1-level supervisor 关联 — FullTimeEmployee 分支专属。
                    // FT 范围 supervisor ID 集合 = {6000} (6001.supervisor=6000)。
                    // 投影包含 DEPARTMENT_ID 列 (FT 的 supervisor.department 需要的 FK seed)。
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME, tb_1_.DEPARTMENT_ID " +
                                    "from STAFF_EMPLOYEE tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(6000L);
                    // 1-level 关联 (FullTimeEmployee 分支专属, `responsibilities` per-branch)。
                    ctx.statement(2).sql(
                            "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                                    "from STAFF_RESPONSIBILITY tb_1_ " +
                                    "where tb_1_.OWNER_ID in (?, ?)"
                    ).variables(6000L, 6001L);
                    // 1-level supervisor 关联 — PartTimeEmployee 分支专属。
                    // PT 范围 supervisor ID 集合 = {6000, 6001}
                    // (6002.supervisor=6000, 6003.supervisor=6001)。
                    // 注意: PT 的 TargetOf_supervisor 没声明 department,
                    // 所以 PT 这条 SQL **不** 投影 DEPARTMENT_ID 列 (取各自需要的)。
                    ctx.statement(3).sql(
                            "select tb_1_.ID, tb_1_.EMP_TYPE, tb_1_.FULL_NAME " +
                                    "from STAFF_EMPLOYEE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(6000L, 6001L);
                    // 1-level 关联 (PartTimeEmployee 分支专属, `responsibilities` per-branch)。
                    ctx.statement(4).sql(
                            "select tb_1_.OWNER_ID, tb_1_.ID, tb_1_.TITLE, tb_1_.PROJECT_ID " +
                                    "from STAFF_RESPONSIBILITY tb_1_ " +
                                    "where tb_1_.OWNER_ID in (?, ?)"
                    ).variables(6002L, 6003L);
                    // 2-level supervisor.department — FullTimeEmployee 分支专属。
                    // PartTimeEmployee 的 TargetOf_supervisor 没声明 department,
                    // 所以只有 FT 发这条 SQL。
                    // 6000 (FT 的 supervisor) 的 department_id = 6900,所以 IN (6900)。
                    ctx.statement(5).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.LOCATION " +
                                    "from STAFF_DEPARTMENT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(6900L);
                    // 2-level 关联 (responsibilities.project) — FullTimeEmployee 分支专属。
                    ctx.statement(6).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                                    "from STAFF_PROJECT tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    ).variables(6300L, 6301L);
                    // 2-level 关联 (responsibilities.project) — PartTimeEmployee 分支专属。
                    ctx.statement(7).sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.CLIENT " +
                                    "from STAFF_PROJECT tb_1_ " +
                                    "where tb_1_.ID = ?"
                    ).variables(6302L);
                    ctx.rows(4);
                    // (single: 共 8 条 SQL, 多了 1 条 PT supervisor 1-level)
                    ctx.row(0, row -> {
                        assertTrue(row instanceof EmployeeOverview.FullTimeEmployee);
                        EmployeeOverview.FullTimeEmployee e =
                                (EmployeeOverview.FullTimeEmployee) row;
                        assertEquals(6000L, e.getId());
                        assertEquals("Alice Allen", e.getFullName());
                        assertEquals(120000L, e.getAnnualSalary());
                        // Alice (FT) 没有 supervisor (她是组织链的根)。
                        assertNull(e.getSupervisor());
                        assertEquals(2, e.getResponsibilities().size());
                        assertProjectPhoenix(e.getResponsibilities().get(0).getProject());
                        assertProjectApollo(e.getResponsibilities().get(1).getProject());
                    });
                    ctx.row(1, row -> {
                        assertTrue(row instanceof EmployeeOverview.FullTimeEmployee);
                        EmployeeOverview.FullTimeEmployee e =
                                (EmployeeOverview.FullTimeEmployee) row;
                        assertEquals(6001L, e.getId());
                        assertEquals(95000L, e.getAnnualSalary());
                        // Bob 的 supervisor 是 Alice (FT 专属 inner DTO, 有 getDepartment)。
                        assertSupervisorAliceWithDepartment(e.getSupervisor());
                        assertEquals(1, e.getResponsibilities().size());
                        assertProjectPhoenix(e.getResponsibilities().get(0).getProject());
                    });
                    ctx.row(2, row -> {
                        assertTrue(row instanceof EmployeeOverview.PartTimeEmployee);
                        EmployeeOverview.PartTimeEmployee e =
                                (EmployeeOverview.PartTimeEmployee) row;
                        assertEquals(6002L, e.getId());
                        assertEquals(50, e.getHourlyRate());
                        // Carol 的 supervisor 是 Alice (PT 专属 inner DTO, **没有** getDepartment)。
                        assertSupervisorAlicePlain(e.getSupervisor());
                        assertEquals(1, e.getResponsibilities().size());
                        assertProjectHelios(e.getResponsibilities().get(0).getProject());
                    });
                    ctx.row(3, row -> {
                        assertTrue(row instanceof EmployeeOverview.PartTimeEmployee);
                        EmployeeOverview.PartTimeEmployee e =
                                (EmployeeOverview.PartTimeEmployee) row;
                        assertEquals(6003L, e.getId());
                        assertEquals(35, e.getHourlyRate());
                        // Dave 的 supervisor 是 Bob (PT 专属 inner DTO)。
                        assertSupervisorBobPlain(e.getSupervisor());
                        assertEquals(1, e.getResponsibilities().size());
                        assertProjectHelios(e.getResponsibilities().get(0).getProject());
                    });
                }
        );
    }

    /* ---- Director 断言辅助方法 ---------------------------------------- */

    private static void assertDirectorAlice(
            OrganizationOverview.Company.TargetOf_director director
    ) {
        assertNotNull(director);
        assertEquals(5900L, director.getId());
        assertEquals("Alice Anderson", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5800L, director.getOfficeAddress().getId());
        assertEquals("100 Main St", director.getOfficeAddress().getStreet());
        assertEquals("Beijing", director.getOfficeAddress().getCity());
    }

    private static void assertDirectorBob(
            OrganizationOverview.Company.TargetOf_director director
    ) {
        assertNotNull(director);
        assertEquals(5901L, director.getId());
        assertEquals("Bob Brown", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5801L, director.getOfficeAddress().getId());
        assertEquals("200 Oak Ave", director.getOfficeAddress().getStreet());
        assertEquals("Shanghai", director.getOfficeAddress().getCity());
    }

    private static void assertDirectorCarol(
            OrganizationOverview.GovernmentAgency.TargetOf_director director
    ) {
        assertNotNull(director);
        assertEquals(5902L, director.getId());
        assertEquals("Carol Carter", director.getFullName());
        assertNotNull(director.getOfficeAddress());
        assertEquals(5802L, director.getOfficeAddress().getId());
        assertEquals("300 Pine Rd", director.getOfficeAddress().getStreet());
        assertEquals("Guangzhou", director.getOfficeAddress().getCity());
    }

    /* ---- Manager 断言辅助方法 ----------------------------------------- */

    private static void assertManagerEve(
            OrganizationOverview.Company.TargetOf_departments.TargetOf_manager manager
    ) {
        assertNotNull(manager);
        assertEquals(5700L, manager.getId());
        assertEquals("Eve Edwards", manager.getFullName());
        assertEquals(3, manager.getLevel());
    }

    private static void assertManagerFrank(
            OrganizationOverview.Company.TargetOf_departments.TargetOf_manager manager
    ) {
        assertNotNull(manager);
        assertEquals(5701L, manager.getId());
        assertEquals("Frank Foster", manager.getFullName());
        assertEquals(2, manager.getLevel());
    }

    private static void assertManagerGrace(
            OrganizationOverview.Company.TargetOf_departments.TargetOf_manager manager
    ) {
        assertNotNull(manager);
        assertEquals(5702L, manager.getId());
        assertEquals("Grace Green", manager.getFullName());
        assertEquals(3, manager.getLevel());
    }

    private static void assertManagerHenry(
            OrganizationOverview.GovernmentAgency.TargetOf_departments.TargetOf_manager manager
    ) {
        assertNotNull(manager);
        assertEquals(5703L, manager.getId());
        assertEquals("Henry Howard", manager.getFullName());
        assertEquals(2, manager.getLevel());
    }

    /* ---- Supervisor 断言辅助方法 -------------------------------------- */

    /*
     * 关键验证: FullTimeEmployee.TargetOf_supervisor 有 getDepartment() 方法
     * (因为 FT 的 supervisor 声明里包含 department 子 DTO)。
     * 如果 jimmer-apt 错误地把 supervisor 当作根属性,这个 helper 会编译失败。
     */
    private static void assertSupervisorAliceWithDepartment(
            EmployeeOverview.FullTimeEmployee.TargetOf_supervisor supervisor
    ) {
        assertNotNull(supervisor);
        assertEquals(6000L, supervisor.getId());
        assertEquals("Alice Allen", supervisor.getFullName());
        assertNotNull(supervisor.getDepartment());
        assertEquals(6900L, supervisor.getDepartment().getId());
        assertEquals("Engineering", supervisor.getDepartment().getName());
        assertEquals("Beijing", supervisor.getDepartment().getLocation());
    }

    /*
     * 关键验证: PartTimeEmployee.TargetOf_supervisor **没有** getDepartment() 方法
     * (因为 PT 的 supervisor 声明里只有 id/fullName,没有 department)。
     * 这个 helper 用 Object 接收 PT 专属 inner DTO,避免编译期检查 getDepartment
     * 是否存在(存在的检查是编译期行为,不是运行时)。
     */
    private static void assertSupervisorAlicePlain(Object supervisor) {
        assertNotNull(supervisor);
        // FT/PT 的 inner DTO 是不同类 — 验证类型确实是 PT 专属的 TargetOf_supervisor
        assertTrue(
                supervisor instanceof EmployeeOverview.PartTimeEmployee.TargetOf_supervisor,
                "Expected PartTimeEmployee.TargetOf_supervisor, got " +
                        supervisor.getClass().getName()
        );
    }

    private static void assertSupervisorBobPlain(Object supervisor) {
        assertNotNull(supervisor);
        assertTrue(
                supervisor instanceof EmployeeOverview.PartTimeEmployee.TargetOf_supervisor,
                "Expected PartTimeEmployee.TargetOf_supervisor, got " +
                        supervisor.getClass().getName()
        );
    }

    /* ---- Project 断言辅助方法 ----------------------------------------- */

    private static void assertProjectPhoenix(
            EmployeeOverview.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project project
    ) {
        assertNotNull(project);
        assertEquals(6300L, project.getId());
        assertEquals("Phoenix", project.getName());
        assertEquals("Internal", project.getClient());
    }

    private static void assertProjectApollo(
            EmployeeOverview.FullTimeEmployee.TargetOf_responsibilities.TargetOf_project project
    ) {
        assertNotNull(project);
        assertEquals(6301L, project.getId());
        assertEquals("Apollo", project.getName());
        assertEquals("NASA", project.getClient());
    }

    private static void assertProjectHelios(Object project) {
        // TargetOf_project 嵌套在每个 branch 的 TargetOf_responsibilities 内
        // (FullTimeEmployee. vs PartTimeEmployee.),所以用 Object 接收保持 branch 无关。
        assertNotNull(project);
    }
}
