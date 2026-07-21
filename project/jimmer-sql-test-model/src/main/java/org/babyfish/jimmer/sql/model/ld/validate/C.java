package org.babyfish.jimmer.sql.model.ld.validate;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.LogicalDeleted;

@DatabaseValidationIgnore
@Entity
public interface C {

    @Id
    long id();

    @LogicalDeleted("2")
    int state();
}
