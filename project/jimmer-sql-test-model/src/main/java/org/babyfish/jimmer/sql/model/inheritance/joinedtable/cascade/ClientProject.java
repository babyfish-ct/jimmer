package org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "JOINED_CASCADE_PROJECT")
public interface ClientProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @Nullable
    @ManyToOne
    @OnDissociate(DissociateAction.LAX)
    Client client();
}
