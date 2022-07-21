package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Key
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne

@Entity
interface Book {

    @get:Id
    val id: Long

    @get:Key
    val name: String

    @get:Key
    val edition: Int

    val price: BigDecimal

    @get:ManyToOne
    val store: BookStore?

    @get:ManyToMany
    val authors: List<Author>
}