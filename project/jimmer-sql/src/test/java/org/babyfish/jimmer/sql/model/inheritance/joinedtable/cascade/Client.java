package org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_CASCADE_CLIENT")
@Inheritance(
        strategy = InheritanceType.JOINED,
        joinedTableDeleteMode = JoinedTableDeleteMode.DB_CASCADE
)
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    String name();

    @Version
    int version();
}
