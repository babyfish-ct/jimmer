package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.LogicalDeletedLongGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Entity
public interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonConverter(LongToStringConverter.class)
    long id();

    String name();

    @LogicalDeleted
    @Nullable
    UUID deletedUUID();

    @ManyToOne
    @Nullable
    Department department();
}
