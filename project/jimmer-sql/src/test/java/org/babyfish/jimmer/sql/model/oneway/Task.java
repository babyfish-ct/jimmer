package org.babyfish.jimmer.sql.model.oneway;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.ManyToOne;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Task {

    @Id
    long id();

    String name();

    @Nullable
    @ManyToOne
    Worker owner();
}
