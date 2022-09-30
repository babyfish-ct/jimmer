package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.Entity;

@Entity
public interface Role extends RoleBase {

    String getName();
}
