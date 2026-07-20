package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.MapsId;
import org.babyfish.jimmer.sql.OneToOne;
import org.babyfish.jimmer.sql.Table;

@Entity
@Table(name = "LONG_MAPS_ID_PROFILE")
public interface LongMapsIdProfile {

    @Id
    long id();

    String nickname();

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    Tenant tenant();
}
