package org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("TRUCK")
public interface Truck extends Vehicle {

    int payload();
}
