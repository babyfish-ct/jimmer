package org.babyfish.jimmer.sql.model.inheritance.joinedtable.key;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Discriminator;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface KeyClientBase {

    @Key
    @Discriminator
    @Column(name = "CLIENT_TYPE")
    String type();
}
