package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface UnsignedNumber {
    @Id
    val id: ULong

    val unsignedInt: UInt

    val unsignedShort: UShort

    val unsignedByte: UByte
}