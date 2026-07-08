package org.babyfish.jimmer.sql.model.inheritance.key;

import org.babyfish.jimmer.sql.Entity;

@Entity
public interface NaturalPerson extends NaturalClient {

    String firstName();

    String lastName();
}
