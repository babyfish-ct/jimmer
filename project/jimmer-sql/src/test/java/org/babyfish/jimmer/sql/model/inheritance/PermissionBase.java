package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.validation.constraints.Null;

@MappedSuperclass
public interface PermissionBase extends NamedEntity {

    @ManyToOne
    @Null
    Role getRole();
}
