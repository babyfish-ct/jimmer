package org.babyfish.jimmer.sql.model.inheritance.singletable;

import org.babyfish.jimmer.sql.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "CLIENT_TYPE")
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    String type();

    String name();
}
