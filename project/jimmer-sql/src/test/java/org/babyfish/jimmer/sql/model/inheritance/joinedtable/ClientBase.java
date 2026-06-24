package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface ClientBase {

    @Discriminator
    String type();
}
