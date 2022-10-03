package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface NamedEntity {

    @Key
    String getName();
}
