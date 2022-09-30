package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Permission extends PermissionBase {

    @Id
    long getId();

    String getName();
}
