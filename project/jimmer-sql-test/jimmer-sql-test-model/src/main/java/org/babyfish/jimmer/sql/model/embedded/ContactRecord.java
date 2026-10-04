package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface ContactRecord {

    @Id
    long id();

    ContactInfo contact();
}
