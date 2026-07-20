package org.babyfish.jimmer.sql.model.ld.validate;

import org.babyfish.jimmer.sql.*;

@DatabaseValidationIgnore
@Entity
public interface E {

    @Id
    long id();

    @Default("NEW")
    @LogicalDeleted("DELETED")
    State state();
}
