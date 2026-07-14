package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

@Entity(instantiability = EntityInstantiability.ABSTRACT)
@Table(name = "ML_JOINED_VEHICLE")
public interface Vehicle extends Asset {

    String manufacturer();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "OWNER_ID")
    VehicleOwner owner();

    @Nullable
    @IdView
    Long ownerId();
}
