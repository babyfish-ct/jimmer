package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ML_JOINED_TRUCK")
@DiscriminatorValue("TRUCK")
public interface Truck extends Vehicle {

    int payload();
}
