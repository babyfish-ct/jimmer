package org.babyfish.jimmer.sql.model.mapsid;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface DualParentChildId {

    long leftId();

    long rightId();

    long localId();
}
