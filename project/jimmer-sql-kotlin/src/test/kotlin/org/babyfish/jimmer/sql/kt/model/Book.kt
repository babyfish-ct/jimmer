package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Entity
interface Book {

    @Id
    val id: Long

    @Key
    val name: @NotBlank String

    @Key
    val edition: @Positive Int

    val price: @Positive BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    val authors: List<Author>
}