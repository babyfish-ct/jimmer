package org.babyfish.jimmer.sql.kt.model.classic.book

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.classic.author.Author
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

/**
 * The Book Entity, 100% immutable
 */
@Entity
@KeyUniqueConstraint
interface Book {

    /**
     * The id property
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    /**
     * The name property, 100% immutable
     */
    @Key
    val name: @NotEmpty(message = "The book name cannot be empty") String

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

    @Formula(dependencies = ["authors"])
    val authorCount
        get() = authors.size

    @Formula(dependencies = ["authors.firstName", "authors.lastName"])
    val authorFullNames: List<String>
        get() = authors.map { "${it.firstName} ${it.lastName}" }
}
