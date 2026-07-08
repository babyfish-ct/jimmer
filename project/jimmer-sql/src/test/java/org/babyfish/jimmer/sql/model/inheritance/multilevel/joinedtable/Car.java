package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ML_JOINED_CAR")
@DiscriminatorValue("CAR")
public interface Car extends Vehicle {

    int seatCount();
}
