package org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "LOGICAL_JOINED_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "CLIENT_TYPE")
public interface Client {

    @Id
    long id();

    String name();

    @LogicalDeleted("true")
    boolean deleted();
}
