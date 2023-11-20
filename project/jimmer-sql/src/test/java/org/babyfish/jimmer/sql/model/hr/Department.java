package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongConverter;
import org.babyfish.jimmer.jackson.LongListConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public interface Department {

    @Id
    @JsonConverter(LongConverter.class)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @LogicalDeleted("now")
    @Nullable
    LocalDateTime deletedTime();

    @OneToMany(mappedBy = "department")
    List<Employee> employees();

    @IdView("employees")
    List<Long> employeeIds();
}
