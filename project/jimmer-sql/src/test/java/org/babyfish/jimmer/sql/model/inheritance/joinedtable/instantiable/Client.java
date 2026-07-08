package org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable;

import org.babyfish.jimmer.sql.*;

@Entity(instantiability = EntityInstantiability.INSTANTIABLE)
@Table(name = "JOINED_INST_CLIENT")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorValue("CLIENT")
public interface Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();

    String name();
}
