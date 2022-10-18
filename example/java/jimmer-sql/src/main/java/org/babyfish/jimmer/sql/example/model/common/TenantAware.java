package org.babyfish.jimmer.sql.example.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.validation.constraints.Null;

@MappedSuperclass
public interface TenantAware extends CommonEntity {

    String tenant();
}
