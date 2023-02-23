package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;

@Entity
public interface B {

    @Id
    long id();

    @LogicalDeleted(value = "DISABLED", restoredValue = "ENABLED")
    Status status();

    enum Status {
        ENABLED,
        DISABLED
    }
}
