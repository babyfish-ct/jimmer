package org.babyfish.jimmer.sql.model.inheritance2;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface BaseEntity {

    @Id
    long id();
}
