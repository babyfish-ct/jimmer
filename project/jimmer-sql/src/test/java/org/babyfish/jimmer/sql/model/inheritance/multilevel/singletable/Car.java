package org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("CAR")
public interface Car extends Vehicle {

    int seatCount();
}
