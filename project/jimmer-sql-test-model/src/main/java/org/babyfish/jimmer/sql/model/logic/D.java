package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@Entity
@Table(name ="JIMMER_TEST_DB.D.TABLE_D")
public interface D {

    @Id
    long id();

    @Nullable
    @LogicalDeleted("null")
    LocalDateTime createdTime();
}
