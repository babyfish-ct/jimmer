package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

@Entity
public interface MapsIdMessage {

    @Id
    long id();

    String text();
}
