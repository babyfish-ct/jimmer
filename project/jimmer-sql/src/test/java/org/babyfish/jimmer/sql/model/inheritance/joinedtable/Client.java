package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
public interface Client extends ClientBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();
}
