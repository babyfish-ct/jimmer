package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.OneToMany;

import java.util.List;

@MappedSuperclass
public interface RoleBase {

    @Id
    long getId();

    @OneToMany(mappedBy = "role")
    List<Permission> getPermissions();
}
