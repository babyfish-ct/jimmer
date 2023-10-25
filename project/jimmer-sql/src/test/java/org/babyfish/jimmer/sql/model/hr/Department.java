package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public interface Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @LogicalDeleted("now")
    @Nullable
    LocalDateTime deletedTime();

    @OneToMany(mappedBy = "department")
    List<Employee> employees();
}
