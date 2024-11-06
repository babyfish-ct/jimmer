package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String account();

    @Key(group = "2")
    String email();

    @Key(group = "3")
    String area();

    @Key(group = "3")
    String nickName();

    String description();
}
