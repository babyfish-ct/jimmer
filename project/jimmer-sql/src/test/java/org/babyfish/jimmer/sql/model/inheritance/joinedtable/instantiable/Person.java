package org.babyfish.jimmer.sql.model.inheritance.joinedtable.instantiable;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "JOINED_INST_PERSON")
public interface Person extends Client {

    String firstName();

    String lastName();
}
