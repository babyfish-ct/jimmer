package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Entity
@KeyUniqueConstraint(noMoreUniqueConstraints = true)
public interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonConverter(LongToStringConverter.class)
    long id();

    @Key
    String name();

    @LogicalDeleted
    long deletedMillis();

    @ManyToOne
    @Nullable
    @JoinColumn(foreignKeyType = ForeignKeyType.FAKE)
    Department department();
}
