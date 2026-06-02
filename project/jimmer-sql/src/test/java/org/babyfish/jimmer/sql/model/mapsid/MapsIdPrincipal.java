package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.*;

@Entity
public interface MapsIdPrincipal {

    @Id
    @PropOverride(prop = "a", columnName = "A")
    @PropOverride(prop = "b", columnName = "B")
    SharedId id();

    String name();
}
