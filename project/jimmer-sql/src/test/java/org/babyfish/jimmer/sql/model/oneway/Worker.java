package org.babyfish.jimmer.sql.model.oneway;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Worker {

    @Id
    long id();

    String name();
}
