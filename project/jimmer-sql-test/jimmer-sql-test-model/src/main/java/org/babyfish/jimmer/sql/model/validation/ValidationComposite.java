package org.babyfish.jimmer.sql.model.validation;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.PropOverride;
import org.babyfish.jimmer.sql.model.embedded.OrderId;

@Entity(microServiceName = "database-validation")
public interface ValidationComposite {

    @Id
    @PropOverride(prop = "x", columnName = "ID_X")
    @PropOverride(prop = "y", columnName = "ID_Y")
    OrderId id();
}
