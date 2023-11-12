package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Entity
public interface File {

    @Id
    @GeneratedValue(sequenceName = "FILE_ID_SEQ")
    long id();

    @Key
    String name();

    @Nullable
    @ManyToOne
    File parent();

    FileType type();

    @OneToMany(mappedBy = "parent", orderedProps = @OrderedProp("name"))
    List<File> subFiles();

    @ManyToMany
    List<User> authorizedUsers();
}
