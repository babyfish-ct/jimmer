package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.sql.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Entity
public interface Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    /**
     * Embeddable property
     */
    @Key
    Location location();

    @Nullable
    @PropOverride(prop = "host", columnName = "SECONDARY_HOST")
    @PropOverride(prop = "port", columnName = "SECONDARY_PORT")
    Location secondaryLocation();

    @Formula(dependencies = {"location.host", "secondaryLocation.host"})
    default List<String> hosts() {
        List<String> hosts = new ArrayList<>();
        hosts.add(location().host());
        Location secondaryLocation = secondaryLocation();
        if (secondaryLocation != null) {
            hosts.add(secondaryLocation.host());
        }
        return hosts;
    }

    int cpuFrequency();

    int memorySize();

    int diskSize();

    MachineDetail detail();

    @Formula(dependencies = {"detail.factories"})
    default int factoryCount() {
        return detail().factories().size();
    }

    @Formula(dependencies = {"detail.factories"})
    default Set<String> factoryNames() {
        Map<String, String> factories = detail().factories();
        return new LinkedHashSet<>(factories.keySet());
    }
}
