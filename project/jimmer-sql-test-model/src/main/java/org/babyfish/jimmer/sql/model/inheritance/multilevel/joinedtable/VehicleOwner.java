package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "ML_JOINED_VEHICLE_OWNER")
public interface VehicleOwner {

    @Id
    long id();

    String name();
}
