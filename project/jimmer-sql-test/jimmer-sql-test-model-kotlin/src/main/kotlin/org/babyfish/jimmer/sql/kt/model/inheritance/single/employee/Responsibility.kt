package org.babyfish.jimmer.sql.kt.model.inheritance.single.employee

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

// 职责 — 分配给员工的一个工作单元。
// 作为 `Employee` (正式或兼职) 的 1-level 分支专属关联使用。
// 每个职责恰好属于一个项目(2-level 关联)。
@Entity
@Table(name = "STAFF_RESPONSIBILITY")
interface Responsibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val title: String

    @ManyToOne
    val owner: Employee?

    @ManyToOne
    val project: Project?
}
