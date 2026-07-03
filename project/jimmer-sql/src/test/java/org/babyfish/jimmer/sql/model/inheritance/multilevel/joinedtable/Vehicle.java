package org.babyfish.jimmer.sql.model.inheritance.multilevel.joinedtable;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.EntityInstantiability;
import org.babyfish.jimmer.sql.Table;

@Entity(instantiability = EntityInstantiability.ABSTRACT)
@Table(name = "ML_JOINED_VEHICLE")
public interface Vehicle extends Asset {

    String manufacturer();
}
