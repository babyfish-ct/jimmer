package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;

@Entity
public interface MapsIdMessageDelivery
        extends MapsIdMessageDeliveryIdBase, MapsIdMessageDeliveryMessageBase {

    String status();
}
