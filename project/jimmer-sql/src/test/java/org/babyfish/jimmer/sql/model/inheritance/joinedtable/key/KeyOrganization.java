package org.babyfish.jimmer.sql.model.inheritance.joinedtable.key;

import org.babyfish.jimmer.sql.DiscriminatorValue;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "JOINED_KEY_ORGANIZATION")
@DiscriminatorValue("ORG")
public interface KeyOrganization extends KeyClient {

    String taxCode();
}
