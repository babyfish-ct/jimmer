package org.babyfish.jimmer.sql.model.enumeration;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;

@DatabaseValidationIgnore
@Entity
public interface Writer {

    @Id
    @GeneratedValue
    long id();

    String name();

    Gender gender();
}
