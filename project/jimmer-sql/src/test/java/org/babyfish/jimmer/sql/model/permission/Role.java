package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Role extends RoleBase {

    @Id
    long getId();

    String getName();
}
