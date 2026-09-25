package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.babyfish.jimmer.sql.KeyUniqueConstraint;

import java.time.LocalDate;

@Entity
@KeyUniqueConstraint
public interface SaveReturningKeyEntity {

    @Id
    long id();

    @Key
    LocalDate dateKey();

    @Key
    SaveReturningKeyEnum enumKey();

    String valueText();
}
