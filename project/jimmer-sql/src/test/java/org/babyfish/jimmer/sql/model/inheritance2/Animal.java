package org.babyfish.jimmer.sql.model.inheritance2;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Key;

@Entity
public interface Animal extends BaseEntity {

    @Key
    String name();
}
