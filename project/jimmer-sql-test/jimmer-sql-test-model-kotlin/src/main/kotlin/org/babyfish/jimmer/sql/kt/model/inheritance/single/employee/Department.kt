package org.babyfish.jimmer.sql.kt.model.inheritance.single.employee

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

// 普通(非 polymorphic)实体 — 部门。
// 作为 polymorphic 根 `supervisor` 关联的 2-level 关联。
// 存放在自己的表 (STAFF_DEPARTMENT) 中,这样 inner DTO
// `TargetOf_department` 嵌套在外部 DTO 层(而不是任何 branch 内)。
@Entity
@Table(name = "STAFF_DEPARTMENT")
interface Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    val location: String
}
