package org.babyfish.jimmer.sql.model.exclude;

import org.babyfish.jimmer.sql.*;

@Entity
@DatabaseValidationIgnore
public interface User {

    @Id
    long id();

    @Key
    String name();

    @Key
    @Key(group = "2")
    String nickName();

    @Key(group = "2")
    @ExcludeFromAllScalars
    String password();
}
