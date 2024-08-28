package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    UnitTypeWrapper typeWrapper();
}
