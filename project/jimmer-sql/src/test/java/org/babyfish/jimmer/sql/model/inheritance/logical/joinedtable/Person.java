package org.babyfish.jimmer.sql.model.inheritance.logical.joinedtable;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "LOGICAL_JOINED_PERSON")
public interface Person extends Client {

    String firstName();

    String lastName();
}
