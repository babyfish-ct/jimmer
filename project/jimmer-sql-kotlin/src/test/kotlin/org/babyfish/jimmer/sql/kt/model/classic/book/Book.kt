package org.babyfish.jimmer.sql.kt.model.classic.book

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

/**
 * The Book Entity
 */
@Entity
interface Book {

    /**
     * The id property
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    /**
     * The name property
     */
    @Key
    @get:NotEmpty(message = "The book name cannot be empty")
    val name: String

    /**
     * The edition property
     */
    @Key
    val edition: @PositiveOrZero Int

    /**
     * The price property
     */
    val price: @Positive BigDecimal

    /**
     * The store property
     */
    @ManyToOne
    val store: BookStore?

    /**
     * The authors property
     */
    @ManyToMany
    val authors: List<Author>

    /**
     * The id view property of `store`
     */
    @IdView
    val storeId: Long?

    /**
     * The id view property of `authorIds`
     */
    @IdView("authors")
    val authorIds: List<Long>
}