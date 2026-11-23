package org.babyfish.jimmer.nojackson;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.jspecify.annotations.Nullable;

@Entity
public interface Note {

    @Id
    long id();

    @Nullable
    String label();
}
