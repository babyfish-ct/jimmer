package org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "JOINED_CASCADE_CLIENT")
@Inheritance(
        strategy = InheritanceType.JOINED,
        joinedTableDissociateAction = JoinedTableDissociateAction.LAX
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

    @OneToMany(mappedBy = "client")
    List<ClientProject> projects();
}
