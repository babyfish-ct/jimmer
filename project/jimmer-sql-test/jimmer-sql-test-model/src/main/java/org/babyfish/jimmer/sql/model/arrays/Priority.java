package org.babyfish.jimmer.sql.model.arrays;

import org.babyfish.jimmer.sql.EnumType;

@EnumType(EnumType.Strategy.ORDINAL)
public enum Priority {
    LOW,
    MEDIUM,
    HIGH
}
