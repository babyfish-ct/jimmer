package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.sql.*;

import java.util.List;

@Entity
public interface Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @OneToMany(mappedBy = "department")
    List<Employee> employees();
}
