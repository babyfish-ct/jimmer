package org.babyfish.jimmer.sql2.example.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface TenantAware extends BaseEntity {

    String tenant();
}
