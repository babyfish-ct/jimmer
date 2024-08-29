package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface Administrator extends AdministratorBase, UserInfo {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();
}
