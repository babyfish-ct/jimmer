package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface PermissionBase extends NamedEntity {

    @ManyToOne
    Role getRole();
}
