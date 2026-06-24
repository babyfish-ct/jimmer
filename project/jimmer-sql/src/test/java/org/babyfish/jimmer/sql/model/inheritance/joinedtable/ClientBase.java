package org.babyfish.jimmer.sql.model.inheritance.joinedtable;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface ClientBase {

    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();
}
