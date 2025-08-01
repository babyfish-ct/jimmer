package org.babyfish.jimmer.sql.model.ld.validate;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@DatabaseValidationIgnore
@Entity
public interface InstantEntity {

    @Id
    long id();

    @Nullable
    @LogicalDeleted("now")
    Instant deletedTime();
}
