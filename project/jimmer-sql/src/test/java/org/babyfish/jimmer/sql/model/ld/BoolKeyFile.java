package org.babyfish.jimmer.sql.model.ld;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface BoolKeyFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String path();

    String name();

    @LogicalDeleted("true")
    boolean deleted();
}
