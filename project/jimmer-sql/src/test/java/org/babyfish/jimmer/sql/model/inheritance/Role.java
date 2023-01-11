package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.pojo.StaticType;
import org.babyfish.jimmer.sql.*;

@Entity
@StaticType(alias = "default", topLevelName = "TheRoleInput")
public interface Role extends RoleBase {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
