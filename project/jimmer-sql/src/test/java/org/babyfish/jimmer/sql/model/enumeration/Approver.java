package org.babyfish.jimmer.sql.model.enumeration;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.jetbrains.annotations.Nullable;

@DatabaseValidationIgnore
@Entity
public interface Approver {

    @Id
    @GeneratedValue
    long id();

    String name();

    @Nullable
    Gender gender();
}
