package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface MapsIdMessageDeliveryIdBase {

    @Id
    long messageId();
}
