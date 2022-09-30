package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface PermissionBase {

    @ManyToOne
    Role getRole();
}
