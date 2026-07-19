package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ML_JOINED_CAR")
@DiscriminatorValue("CAR")
public interface Car extends Vehicle {

    int seatCount();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "DRIVER_ID")
    VehicleOwner driver();
}
