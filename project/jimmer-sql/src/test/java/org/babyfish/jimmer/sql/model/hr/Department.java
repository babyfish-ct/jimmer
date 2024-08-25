package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.jackson.LongToStringConverter;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@KeyUniqueConstraint(noMoreUniqueConstraints = true)
public interface Department {

    @Id
    @JsonConverter(LongToStringConverter.class)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @LogicalDeleted
    long deletedMillis();

    @OneToMany(mappedBy = "department")
    List<Employee> employees();

    @IdView("employees")
    List<Long> employeeIds();

    @Formula(sql = "(select count(*) from employee where department_id = %alias.id)")
    long employeeCount();
}
