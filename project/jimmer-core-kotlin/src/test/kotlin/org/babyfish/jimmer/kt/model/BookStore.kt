package org.babyfish.jimmer.kt.model

import org.babyfish.jimmer.Immutable
import java.math.BigDecimal
import javax.validation.constraints.Pattern
import javax.validation.constraints.PositiveOrZero

@Immutable
interface BookStore {

    @get:Pattern(regexp = "[^\\d]+\\S+")
    val name: String

    val books: List<Book>

    @get:PositiveOrZero
    val avgPrice: BigDecimal?
}