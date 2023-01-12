package org.babyfish.jimmer.sql.example.model.common;

import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface TenantAware extends BaseEntity {

    @Static(enabled = false)
    String tenant();
}
