package org.babyfish.jimmer.sql.model.permission;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Permission extends PermissionBase {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();

    @Key
    String getName();
}
