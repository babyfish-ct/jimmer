package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "JOINED_CLIENT_PROJECT")
public interface ClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @Nullable
    @ManyToOne
    Client client();
}
