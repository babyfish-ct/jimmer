package org.babyfish.jimmer.sql.model.filter;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
@Table(name = "file_user")
public interface User {

    @Id
    long id();

    @Key
    String name();

    @ManyToMany(mappedBy = "users")
    List<File> files();
}
