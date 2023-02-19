package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Entity
interface Book {

    @Id
    val id: Long

    @Key
    val name: String

    @Key
    val edition: @PositiveOrZero Int

    val price: @Positive BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    val authors: List<Author>
}