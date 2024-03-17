package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

@Entity
public interface Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    Location location();

    @Nullable
    @PropOverride(prop = "host", columnName = "SECONDARY_HOST")
    @PropOverride(prop = "port", columnName = "SECONDARY_PORT")
    Location secondaryLocation();

    int cpuFrequency();

    int memorySize();

    int diskSize();

    MachineDetail detail();
}
