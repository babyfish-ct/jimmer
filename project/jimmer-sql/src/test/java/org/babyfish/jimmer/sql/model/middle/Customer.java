package org.babyfish.jimmer.sql.model.middle;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface Customer {

    @Id
    long id();

    String name();
}
