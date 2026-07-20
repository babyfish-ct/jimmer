package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id

@Entity
interface Primitive {

    @Id
    val id: Long

    val booleanValue: Boolean

    val booleanRef: Boolean?

    val charValue: Char

    val charRef: Char?

    val byteValue: Byte

    val byteRef: Byte?

    val shortValue: Short

    val shortRef: Short?

    val intValue: Int

    val intRef: Int?

    val longValue: Long

    val longRef: Long?

    val floatValue: Float

    val floatRef: Float?

    val doubleValue: Double

    val doubleRef: Double?
}