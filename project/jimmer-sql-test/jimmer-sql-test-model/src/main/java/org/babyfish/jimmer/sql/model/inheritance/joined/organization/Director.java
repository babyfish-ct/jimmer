package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.GenerationType;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

/*
 * 普通(非 polymorphic)实体 — 组织的法人代表 / 负责人。
 * 作为 polymorphic 根 Organization 的 1-level 关联使用。
 * 每个 Organization 都有一个 director。
 *
 * `officeAddress` 是 2-level 关联且可空,用于让 runtime 测试
 * 验证 LEFT JOIN 的 nullable 语义。
 */
@Entity
@Table(name = "ORG_DIRECTOR")
public interface Director {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String fullName();

    @ManyToOne
    @Nullable
    Address officeAddress();
}
