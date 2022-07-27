package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public interface IdAndName extends Named {

    @Id
    UUID id();
}
