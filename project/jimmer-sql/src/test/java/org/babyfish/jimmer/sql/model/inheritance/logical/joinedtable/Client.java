package org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "LOGICAL_JOINED_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
public interface Client {

    @Id
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    String name();

    @LogicalDeleted("true")
    boolean deleted();
}
