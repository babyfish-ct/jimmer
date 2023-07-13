package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name ="JIMMER_TEST_DB.A.TABLE_")
public interface A {

    @Id
    long id();

    @LogicalDeleted(value = "1", restoredValue = "0")
    int deleted();
}
