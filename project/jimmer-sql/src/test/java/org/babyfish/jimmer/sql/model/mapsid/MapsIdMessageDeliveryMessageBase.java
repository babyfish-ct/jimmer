package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.MapsId;
import org.babyfish.jimmer.sql.OneToOne;

@MappedSuperclass
public interface MapsIdMessageDeliveryMessageBase {

    @MapsId
    @OneToOne
    @JoinColumn
    MapsIdMessage message();
}
