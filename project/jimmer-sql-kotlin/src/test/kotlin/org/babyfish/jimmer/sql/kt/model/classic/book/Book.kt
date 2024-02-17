package org.babyfish.jimmer.sql.kt.model.classic.book

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

typealias ID = Long

/**
 * The Book Entity, 100% immutable
 */
@Entity
interface Book {

    /**
     * The id property
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: ID

    /**
     * The name property, 100% immutable
     */
    @Key
    @get:NotEmpty(message = "The book name cannot be empty")
    val name: String

    /**
     * The edition property, 100% immutable
     */
    @Key
    val edition: @PositiveOrZero Int

    /**
     * The price property, 100% immutable
     */
    val price: @Positive BigDecimal

    /**
     * The store property, 100% immutable
     */
    @ManyToOne
    val store: BookStore?

    /**
     * The authors property, 100% immutable
     */
    @ManyToMany
    @JoinTable(deletedWhenEndpointIsLogicallyDeleted = true)
    val authors: List<Author>

    /**
     * The id view property of `store`, 100% immutable
     */
    @IdView
    val storeId: Long?

    /**
     * The id view property of `authorIds`, 100% immutable
     */
    @IdView("authors")
    val authorIds: List<Long>
}