package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.jspecify.annotations.Nullable;

@MappedSuperclass
public interface PermissionBase extends NamedEntity {

    @ManyToOne
    @Nullable
    Role getRole();
}
