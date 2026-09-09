package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Key;
import org.jspecify.annotations.Nullable;

@Entity
@DatabaseValidationIgnore
public interface NamedKeyUser {

    @Id
    long id();

    @Key(group = "account")
    String account();

    @Nullable
    String description();
}
