package org.babyfish.jimmer.sql.model.idonly;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ID_ONLY_DOCUMENT")
public interface Document {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Nullable
    String name();

    @OneToMany(mappedBy = "document")
    List<File> files();
}
