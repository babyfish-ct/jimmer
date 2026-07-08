package org.babyfish.jimmer.sql.model.inheritance.singletable;

import org.babyfish.jimmer.sql.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    String name();
}
