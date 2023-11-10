package org.babyfish.jimmer.sql.model.filter;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
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

    @LogicalDeleted(value = "now")
    @Nullable
    LocalDateTime deletedTime();
}
