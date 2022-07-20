package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import java.math.BigDecimal

@Immutable
interface Book {
    val name: String
    val edition: Int
    val price: BigDecimal
    val store: BookStore?
}