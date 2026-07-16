package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface ExcludedDefaultTarget {

    @Id
    long id();
}
