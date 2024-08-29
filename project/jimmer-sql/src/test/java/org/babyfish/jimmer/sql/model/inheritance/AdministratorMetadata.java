package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface AdministratorMetadata extends AdministratorMetadataBase, UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long getId();
}
