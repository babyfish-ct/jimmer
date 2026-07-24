package org.babyfish.jimmer.sql.kt.model.ld.validation

import org.babyfish.jimmer.sql.EnumType

@EnumType(EnumType.Strategy.ORDINAL)
enum class State(val text: String) {
    NEW("st_001"),
    PROCESSING("st_002"),
    DELETED("st_003")
}