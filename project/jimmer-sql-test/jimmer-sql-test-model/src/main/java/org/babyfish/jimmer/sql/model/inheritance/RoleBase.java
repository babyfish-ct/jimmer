package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.calc.RolePermissionCountResolver;

import java.util.List;

@MappedSuperclass
public interface RoleBase extends NamedEntity {

    @OneToMany(mappedBy = "role")
    List<Permission> getPermissions();

    @ManyToMany(mappedBy = "roles")
    List<Administrator> administrators();

    @Transient(RolePermissionCountResolver.class)
    int getPermissionCount();

    @IdView("permissions")
    List<Long> permissionIds();
}
