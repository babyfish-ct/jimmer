package org.babyfish.jimmer.sql.model.logic;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name ="JIMMER_TEST_DB.A.TABLE_A")
public interface A {

    @Id
    long id();

    @LogicalDeleted(value = "1")
    int deleted();
}
