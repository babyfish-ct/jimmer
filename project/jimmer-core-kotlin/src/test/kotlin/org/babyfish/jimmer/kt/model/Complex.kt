package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable

@Immutable
interface Complex {

    val real: Double

    val image: Double
}