package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.MapsId;
import org.babyfish.jimmer.sql.OneToOne;

@Entity
public interface MapsIdMessageDelivery {

    @Id
    long messageId();

    String status();

    @MapsId
    @OneToOne
    @JoinColumn
    MapsIdMessage message();
}
