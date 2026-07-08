package org.babyfish.jimmer.sql.model.inheritance.joinedtable.key;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "JOINED_KEY_PERSON")
public interface KeyPerson extends KeyClient {

    String firstName();

    String lastName();
}
