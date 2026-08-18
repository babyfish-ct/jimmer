package org.babyfish.jimmer.sql.kt.model.inheritance.single.employee

import org.babyfish.jimmer.sql.DiscriminatorValue
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Table

// `Employee` 的 single-table-inheritance 子类型 — 正式员工。
// 没有子类型专属字段 (`annualSalary` 标量存在根表上,
// 这就是单表继承的惯例)。
@Entity
@Table(name = "STAFF_EMPLOYEE")
@DiscriminatorValue("FULL_TIME")
interface FullTimeEmployee : Employee
