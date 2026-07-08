package org.babyfish.jimmer.sql.model.inheritance.multilevel.singletable;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.EntityInstantiability;

@Entity(instantiability = EntityInstantiability.ABSTRACT)
public interface Vehicle extends Asset {

    String manufacturer();
}
