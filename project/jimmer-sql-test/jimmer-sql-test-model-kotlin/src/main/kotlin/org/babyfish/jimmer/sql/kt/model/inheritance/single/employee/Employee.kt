package org.babyfish.jimmer.sql.kt.model.inheritance.single.employee

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Discriminator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Inheritance
import org.babyfish.jimmer.sql.InheritanceType
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.Table

// 员工(Employee)的 single-table-inheritance 抽象根。
// 两个具体子类型: `FullTimeEmployee`(正式员工) 和 `PartTimeEmployee`(兼职员工)。
// 鉴别器列 `EMP_TYPE` 直接定义在根接口上(单表约定,不需要 MappedSuperclass)。
//
// 子类型专属的标量字段(annualSalary / hourlyRate)都存放在根表
// (STAFF_EMPLOYEE)上,只在对应子类型中有值,另一侧为 null。
//
// 根属性(每个子类型共享):
//   - `supervisor` (1-level, 自关联 ManyToOne) + `supervisor.department` (2-level)
//     -> inner DTO 嵌套在外部 DTO 层
//
// 分支专属属性(在 `#types` 内声明):
//   - `responsibilities` (1-level, OneToMany) + `responsibilities.project` (2-level)
//     -> inner DTO 嵌套在每个 branch 类内
@Entity
@Table(name = "STAFF_EMPLOYEE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val fullName: String

    @Discriminator
    @Column(name = "EMP_TYPE")
    val type: String

    // 自关联 1-level 关联:每个员工有一个直接领导。
    // polymorphic DTO 把它作为根属性投影
    // (inner DTO `TargetOf_supervisor` 嵌套在外部 DTO 层)。
    @ManyToOne
    val supervisor: Employee?

    // 这个员工所属的部门。独立于 `supervisor`
    // (每个员工都有自己的部门,即使与领导同部门)。
    // polymorphic DTO 把它作为 `supervisor.department` 的 2-level 关联投影。
    @ManyToOne
    val department: Department?

    @OneToMany(mappedBy = "owner")
    val responsibilities: List<Responsibility>

    // single-table 继承约定:子类型专属标量字段
    // (FullTime 的 annualSalary / PartTime 的 hourlyRate)
    // 都存放在这张根表上,各子类型独有的字段在另一子类型为 null。
    val annualSalary: Long?

    val hourlyRate: Int?
}
