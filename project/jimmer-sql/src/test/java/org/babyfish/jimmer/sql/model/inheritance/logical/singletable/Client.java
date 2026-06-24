package org.babyfish.jimmer.sql.model.inheritance.logical.singletable;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "LOGICAL_CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "CLIENT_TYPE")
public interface Client {

    @Id
    long id();

    String name();

    @LogicalDeleted("true")
    boolean deleted();
}
