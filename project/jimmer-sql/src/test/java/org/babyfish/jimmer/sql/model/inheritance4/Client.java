package org.babyfish.jimmer.sql.model.inheritance4;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "CLIENT_TYPE")
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();
}
