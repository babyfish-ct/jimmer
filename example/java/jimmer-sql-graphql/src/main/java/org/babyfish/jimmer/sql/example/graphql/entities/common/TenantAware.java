package org.babyfish.jimmer.sql.example.graphql.entities.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface TenantAware extends CommonEntity {

    String tenant();
}
