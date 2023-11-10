package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

@Entity
public interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @LogicalDeleted("now")
    @Nullable
    LocalDateTime deletedTime();

    @ManyToOne
    @Nullable
    Department department();
}
