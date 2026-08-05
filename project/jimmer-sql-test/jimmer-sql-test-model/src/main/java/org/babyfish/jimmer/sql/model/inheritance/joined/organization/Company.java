package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

/*
 * `Organization` 的 joined-inheritance 子类型 — 私有公司。
 * 携带 `shareCount`(股份数)标量字段,该字段存放在子表 (ORG_COMPANY) 上。
 */
@Entity
@Table(name = "ORG_COMPANY")
@DiscriminatorValue("COMPANY")
public interface Company extends Organization {

    int shareCount();
}
