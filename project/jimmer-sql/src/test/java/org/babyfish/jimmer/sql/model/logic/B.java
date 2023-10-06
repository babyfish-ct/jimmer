package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name ="JIMMER_TEST_DB.B.TABLE_")
public interface B {

    @Id
    long id();

    @LogicalDeleted(value = "DISABLED")
    Status status();

    enum Status {
        ENABLED,
        DISABLED
    }
}
