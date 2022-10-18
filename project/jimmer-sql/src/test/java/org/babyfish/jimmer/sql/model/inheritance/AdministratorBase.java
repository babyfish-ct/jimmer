package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@MappedSuperclass
public interface AdministratorBase extends NamedEntity {

    @ManyToMany
    @JoinTable(
            name = "ADMINISTRATOR_ROLE_MAPPING",
            joinColumnName = "ADMINISTRATOR_ID",
            inverseJoinColumnName = "ROLE_ID"
    )
    List<Role> getRoles();

    @OneToOne(mappedBy = "administrator")
    AdministratorMetadata getMetadata();
}
