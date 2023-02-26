package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;

@Entity
public interface A {

    @Id
    long id();

    @LogicalDeleted(value = "1", restoredValue = "0")
    int deleted();
}
