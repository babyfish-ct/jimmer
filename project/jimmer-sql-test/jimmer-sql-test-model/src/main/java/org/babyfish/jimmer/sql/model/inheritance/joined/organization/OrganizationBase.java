package org.babyfish.jimmer.sql.model.inheritance.joined.organization;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.MappedSuperclass;

/*
 * joined-inheritance 的 @Discriminator 列载体 (MappedSuperclass)。
 * 具体的根实体 `Organization` 在这里用 JOINED 策略声明继承关系。
 * 鉴别器列名: ORG_TYPE
 */
@MappedSuperclass
public interface OrganizationBase {

    @Discriminator
    @Column(name = "ORG_TYPE")
    String type();
}
