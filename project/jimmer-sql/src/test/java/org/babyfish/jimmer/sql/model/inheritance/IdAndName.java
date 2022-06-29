package org.babyfish.jimmer.sql.model.inheritance;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public interface IdAndName extends Named {

    @Id
    UUID id();
}
