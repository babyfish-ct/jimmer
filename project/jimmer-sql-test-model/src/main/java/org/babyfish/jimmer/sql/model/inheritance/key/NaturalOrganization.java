package org.babyfish.jimmer.sql.model.inheritance.key;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("ORG")
public interface NaturalOrganization extends NaturalClient {

    String taxCode();
}
