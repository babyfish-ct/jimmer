package org.babyfish.jimmer.sql.model.inheritance.joinedtable.cascade;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "JOINED_CASCADE_ORGANIZATION")
@DiscriminatorValue("ORG")
public interface Organization extends Client {

    String taxCode();
}
