package org.babyfish.jimmer.sql.model.filter;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;
import testpkg.annotations.Serializable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file_user")
@Serializable(with = User.class)
public interface User {

    @Id
    long id();

    @Key
    @Serializable(with = String.class)
    String name();

    @ManyToMany(mappedBy = "users")
    List<File> files();

    @LogicalDeleted(value = "now")
    @Nullable
    LocalDateTime deletedTime();
}
