package org.babyfish.jimmer.client.java.model.inheritance;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;

@Entity
@DiscriminatorValue("ORGANIZATION")
public interface Organization extends Client {

    String taxCode();
}
