package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

/*
 * 普通(非 polymorphic)实体 — 地址。
 * 作为 polymorphic 根 1-level 关联 `director` 的 2-level 关联。
 * 存放在自己的表 (ORG_ADDRESS),这样 inner DTO `TargetOf_address`
 * 嵌套在外部 DTO 层(而不是任何 branch 内)。
 */
@Entity
@Table(name = "ORG_ADDRESS")
public interface Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String street();

    String city();
}
