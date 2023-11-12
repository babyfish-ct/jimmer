package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "FILE_USER")
public interface User {

    @GeneratedValue(sequenceName = "FILE_USER_ID_SEQ")
    @Id
    long id();

    @Key
    String nickName();
}
