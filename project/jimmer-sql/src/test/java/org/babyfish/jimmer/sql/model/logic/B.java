package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name ="JIMMER_TEST_DB.B.TABLE_B")
public interface B {

    @Id
    long id();

    @LogicalDeleted(value = "DISABLED")
    Status status();

    @ManyToOne
    @JoinColumn(foreignKeyType = ForeignKeyType.FAKE)
    @Nullable
    A a();

    enum Status {
        ENABLED,
        DISABLED
    }
}
