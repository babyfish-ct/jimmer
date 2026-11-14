package org.babyfish.jimmer.sql.kt.model.pg

import org.babyfish.jimmer.sql.EnumType

@EnumType(EnumType.Strategy.ORDINAL)
enum class Priority {
    LOW,
    MEDIUM,
    HIGH
}
