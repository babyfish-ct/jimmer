package org.babyfish.jimmer.sql.model.validation;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity(microServiceName = "database-validation")
public interface ValidationStore {

    @Id
    long id();

    long code();
}
