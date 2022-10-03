package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Role extends RoleBase {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
