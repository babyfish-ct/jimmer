package org.babyfish.jimmer.sql.model.inheritance.logical.singletable;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "LOGICAL_CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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
