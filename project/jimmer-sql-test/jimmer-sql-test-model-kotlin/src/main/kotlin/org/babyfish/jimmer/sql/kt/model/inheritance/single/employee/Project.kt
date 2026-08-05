package org.babyfish.jimmer.sql.kt.model.inheritance.single.employee

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

// 普通(非 polymorphic)实体 — 项目。
// 作为分支专属 1-level 关联 `responsibilities` 的 2-level 关联。
// 存放在自己的表 (STAFF_PROJECT) 中,这样 `TargetOf_project`
// 嵌套在每个 branch 的 `TargetOf_responsibilities` inner DTO 里。
@Entity
@Table(name = "STAFF_PROJECT")
interface Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    val client: String
}
