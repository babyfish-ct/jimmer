package org.babyfish.jimmer.sql.model.inheritance2;

import org.babyfish.jimmer.sql.Entity;

@Entity
public interface Animal extends BaseEntity {

    String name();
}
