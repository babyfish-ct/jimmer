package org.babyfish.jimmer.sql.model.inheritance.joinedtable.upsert;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "JOINED_UPSERT_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
public interface Client {

    @Id
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    String name();

    @OneToMany(mappedBy = "client")
    List<ClientContact> contacts();
}
