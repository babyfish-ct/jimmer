package org.babyfish.jimmer.sql.model.validation;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity(microServiceName = "database-validation")
public interface ValidationIgnoredId {

    @Id
    @DatabaseValidationIgnore
    long id();
}
