package org.babyfish.jimmer.sql.model.inheritance.key;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "NATURAL_CLIENT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public interface NaturalClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    @Key
    String code();

    String name();
}
