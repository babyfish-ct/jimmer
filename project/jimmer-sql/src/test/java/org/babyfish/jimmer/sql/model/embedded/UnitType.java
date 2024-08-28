package org.babyfish.jimmer.sql.model.embedded;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

@EnumType(EnumType.Strategy.NAME)
public enum UnitType {

    @EnumItem(name = "FM")
    FOOTMAN,

    @EnumItem(name = "RM")
    RIFLEMAN,

    @EnumItem(name = "K")
    KNIGHT,

    @EnumItem(name = "S")
    SORCERESS
}
