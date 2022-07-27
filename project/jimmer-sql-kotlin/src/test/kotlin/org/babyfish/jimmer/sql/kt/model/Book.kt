package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity
interface Book {

    @Id
    val id: Long

    @Key
    val name: String

    @Key
    val edition: Int

    val price: BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    val authors: List<Author>
}