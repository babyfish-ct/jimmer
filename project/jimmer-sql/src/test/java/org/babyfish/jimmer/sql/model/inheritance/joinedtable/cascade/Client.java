package org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_CASCADE_CLIENT")
@Inheritance(
        strategy = InheritanceType.JOINED,
        joinedTableDeleteMode = JoinedTableDeleteMode.DB_CASCADE
)
@DiscriminatorColumn(name = "CLIENT_TYPE")
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();
}
