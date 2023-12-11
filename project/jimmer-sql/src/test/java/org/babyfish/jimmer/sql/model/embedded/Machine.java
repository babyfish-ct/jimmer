package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.*;

@Entity
public interface Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    Location location();

    int cpuFrequency();

    int memorySize();

    int diskSize();
}
