package org.babyfish.jimmer.sql.model.inheritance.singletable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "SINGLE_CLIENT_PROJECT")
public interface ClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @Nullable
    @ManyToOne
    Client client();
}
