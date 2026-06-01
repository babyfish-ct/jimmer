package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.*;

@Entity
public interface DualParentChild {

    @Id
    @PropOverride(prop = "leftId", columnName = "LEFT_ID")
    @PropOverride(prop = "rightId", columnName = "RIGHT_ID")
    @PropOverride(prop = "localId", columnName = "LOCAL_ID")
    DualParentChildId id();

    String name();

    @ManyToOne
    @MapsId("leftId")
    @JoinColumn(name = "LEFT_ID", referencedColumnName = "ID")
    Tenant left();

    @ManyToOne
    @MapsId("rightId")
    @JoinColumn(name = "RIGHT_ID", referencedColumnName = "ID")
    Tenant right();
}
