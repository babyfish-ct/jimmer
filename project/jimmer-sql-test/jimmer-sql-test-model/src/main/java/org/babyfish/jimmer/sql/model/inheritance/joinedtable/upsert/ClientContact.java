package org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "JOINED_UPSERT_CLIENT_CONTACT")
@KeyUniqueConstraint
public interface ClientContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String num();

    String title();

    @ManyToOne
    Client client();
}
