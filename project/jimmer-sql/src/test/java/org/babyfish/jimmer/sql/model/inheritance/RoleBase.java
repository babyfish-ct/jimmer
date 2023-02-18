package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.OneToMany;
import org.babyfish.jimmer.sql.Transient;
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
}
