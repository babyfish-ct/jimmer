package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.Table;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@Entity
@DatabaseValidationIgnore
@Table(name ="JIMMER_TEST_DB.F.TABLE_F")
public interface F {

    @Id
    long id();

    @Nullable
    @LogicalDeleted("null")
    Instant deletedTime();
}
