package org.babyfish.jimmer.sql.model.inheritance.logical.singletable;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("ORG")
public interface Organization extends Client {

    String taxCode();
}
