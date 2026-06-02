package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.*;

@Entity
public interface MapsIdProfile {

    @Id
    @PropOverride(prop = "a", columnName = "A")
    @PropOverride(prop = "b", columnName = "B")
    SharedId id();

    String nickname();

    @OneToOne
    @MapsId
    @JoinColumn(name = "A", referencedColumnName = "A")
    @JoinColumn(name = "B", referencedColumnName = "B")
    MapsIdPrincipal principal();
}
