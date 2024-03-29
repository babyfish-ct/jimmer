package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Permission extends PermissionBase {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
