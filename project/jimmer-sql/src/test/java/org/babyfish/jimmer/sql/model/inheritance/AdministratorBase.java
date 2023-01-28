package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.pojo.AutoScalarRule;
import org.babyfish.jimmer.pojo.AutoScalarStrategy;
import org.babyfish.jimmer.pojo.Static;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@MappedSuperclass
public interface AdministratorBase extends NamedEntity {

    @ManyToMany
    @JoinTable(
            name = "ADMINISTRATOR_ROLE_MAPPING",
            joinColumnName = "ADMINISTRATOR_ID",
            inverseJoinColumnName = "ROLE_ID"
    )
    @Static(targetAlias = "default")
    List<Role> getRoles();

    @OneToOne(mappedBy = "administrator")
    @Nullable
    AdministratorMetadata getMetadata();
}
