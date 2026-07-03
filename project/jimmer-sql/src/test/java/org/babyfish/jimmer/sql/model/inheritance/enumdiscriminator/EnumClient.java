package org.babyfish.jimmer.sql.model.inheritance.enumdiscriminator;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity(instantiability = EntityInstantiability.INSTANTIABLE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("CLIENT")
public interface EnumClient {

    @Id
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    ClientType type();

    @Nullable
    String name();
}
