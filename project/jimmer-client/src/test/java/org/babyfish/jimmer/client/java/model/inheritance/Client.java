package org.babyfish.jimmer.client.java.model.inheritance;

import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Inheritance;
import org.babyfish.jimmer.sql.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public interface Client {

    @Id
    long id();

    @Discriminator
    ClientType type();

    String name();
}
