package org.babyfish.jimmer.sql.model.inheritance;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public interface Named {

    String name();
}
