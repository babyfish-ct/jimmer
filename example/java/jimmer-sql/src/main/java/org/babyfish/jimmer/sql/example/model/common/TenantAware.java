package org.babyfish.jimmer.sql.example.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass // ❶
public interface TenantAware {

    String tenant();
}

/*----------------Documentation Links----------------
❶ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/mapped-super-class
---------------------------------------------------*/
