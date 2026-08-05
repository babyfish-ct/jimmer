package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

/*
 * 部门 — 作为组织 polymorphic 根的 1-level 分支专属关联。
 * 每个组织(Company 或 GovernmentAgency)至少有一个部门,
 * 每个部门有 0 或 1 个经理(Manager)。
 *
 * `manager` 可空,用于让 runtime 测试验证 LEFT JOIN 的 nullable 语义。
 */
@Entity
@Table(name = "ORG_DEPARTMENT")
public interface Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @ManyToOne
    Organization organization();

    @ManyToOne
    @Nullable
    Manager manager();
}
