package org.babyfish.jimmer.sql.model.idonly;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;

import java.util.UUID;

@Entity
@Table(name = "ID_ONLY_FILE")
public interface File {

    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @ManyToOne
    Document document();

    String link();
}
