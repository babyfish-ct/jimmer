package org.babyfish.jimmer.sql.model.inheritance.single.employee;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

/*
 * `Employee` 的 single-table-inheritance 子类型 — 兼职 / 合同工。
 * 没有子类型专属字段 (`hourlyRate` 标量存在根表上)。
 */
@Entity
@Table(name = "STAFF_EMPLOYEE")
@DiscriminatorValue("PART_TIME")
public interface PartTimeEmployee extends Employee {
}
