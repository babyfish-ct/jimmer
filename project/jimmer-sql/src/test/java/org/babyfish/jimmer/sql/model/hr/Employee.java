package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonConverter(LongConverter.class)
    long id();

    String name();

    @LogicalDeleted
    @Nullable
    UUID deletedUUID();

    @ManyToOne
    @Nullable
    Department department();
}
