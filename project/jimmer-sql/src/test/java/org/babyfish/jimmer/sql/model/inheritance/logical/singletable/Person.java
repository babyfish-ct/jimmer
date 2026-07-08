package org.babyfish.jimmer.sql.model.inheritance.logical.singletable;

import org.babyfish.jimmer.sql.Entity;

@Entity
public interface Person extends Client {

    String firstName();

    String lastName();
}
