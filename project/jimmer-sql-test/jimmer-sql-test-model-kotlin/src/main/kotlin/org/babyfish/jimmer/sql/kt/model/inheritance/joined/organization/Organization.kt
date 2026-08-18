package org.babyfish.jimmer.sql.kt.model.inheritance.joined.organization

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.Table

// 组织(Organization)的 joined-inheritance 抽象根。
// 两个具体子类型: `Company`(公司) 和 `GovernmentAgency`(政府机构)。
// 鉴别器列 `ORG_TYPE` 定义在 MappedSuperclass `OrganizationBase` 上。
//
// 根属性(每个子类型共享):
//   - `director` (1-level 关联) + `director.officeAddress` (2-level)
//     -> inner DTO 嵌套在外部 DTO 层
//
// 分支专属属性(在 `#types` 内声明):
//   - `departments` (1-level, OneToMany) + `departments.manager` (2-level)
//     -> inner DTO 嵌套在每个 branch 类内
@Entity
@Table(name = "ORG_ORGANIZATION")
@Inheritance(strategy = InheritanceType.JOINED)
interface Organization : OrganizationBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    @ManyToOne
    val director: Director

    @OneToMany(mappedBy = "organization")
    val departments: List<Department>
}
