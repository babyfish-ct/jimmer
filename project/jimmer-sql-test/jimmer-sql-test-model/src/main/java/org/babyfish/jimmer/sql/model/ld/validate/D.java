package org.babyfish.jimmer.sql.model.ld.validate;

import org.babyfish.jimmer.sql.*;

@DatabaseValidationIgnore
@Entity
public interface D {

    @Id
    long id();

    @Default("1")
    @LogicalDeleted("2")
    int state();
}
