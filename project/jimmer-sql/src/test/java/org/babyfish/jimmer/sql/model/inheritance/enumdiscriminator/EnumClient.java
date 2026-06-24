package org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator;

import org.babyfish.jimmer.sql.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("CLIENT")
public interface EnumClient {

    @Id
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    ClientType type();
}
