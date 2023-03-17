package org.babyfish.jimmer.sql.model.hr;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String name();

    @ManyToOne
    Department department();
}
