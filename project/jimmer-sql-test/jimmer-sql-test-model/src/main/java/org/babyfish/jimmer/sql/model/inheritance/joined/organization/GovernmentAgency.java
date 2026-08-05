package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

/*
 * `Organization` 的 joined-inheritance 子类型 — 公共部门 / 政府机构。
 * 携带 `budget`(年度预算)标量字段,该字段存放在子表
 * (ORG_GOVERNMENT_AGENCY) 上。
 */
@Entity
@Table(name = "ORG_GOVERNMENT_AGENCY")
@DiscriminatorValue("GOV_AGENCY")
public interface GovernmentAgency extends Organization {

    long budget();
}
