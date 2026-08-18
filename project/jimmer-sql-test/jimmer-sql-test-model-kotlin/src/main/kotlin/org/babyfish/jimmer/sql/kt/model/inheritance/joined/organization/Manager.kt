package org.babyfish.jimmer.sql.kt.model.inheritance.joined.organization

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

// 普通(非 polymorphic)实体 — 部门经理。
// 作为 2-level 关联 `departments.manager` 使用。
// 存放在自己的表 (ORG_MANAGER) 中,这样 `TargetOf_manager`
// inner DTO 嵌套在每个 branch 的 `TargetOf_departments` inner DTO 里。
@Entity
@Table(name = "ORG_MANAGER")
interface Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val fullName: String

    // 管理职级: 1=基层, 2=中层, 3=高层。
    // runtime 测试用它来区分公司部门和政府部门中的经理。
    val level: Int
}
