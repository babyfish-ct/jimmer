package org.babyfish.jimmer.sql.model.exclude;

import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.ExcludeFromAllScalars;
import org.babyfish.jimmer.sql.Id;

@Entity
@DatabaseValidationIgnore
public interface User {

    @Id
    long id();

    String name();

    String nickName();

    @ExcludeFromAllScalars
    String password();
}
