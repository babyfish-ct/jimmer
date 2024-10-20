package org.babyfish.jimmer.sql.kt.model.classic.store

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.calc.BookStoreAvgPriceResolver
import org.babyfish.jimmer.sql.kt.model.calc.BookStoreNewestBooksResolver
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.NotBlank

/**
 * The BookStore property, 100% immutable
 */
@Entity
interface BookStore {

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
    val name: @NotBlank String

    /**
     * The version property
     */
    @Version
    val version: Int

    /**
     * The calculated property: avgPrice
     */
    @Transient(BookStoreAvgPriceResolver::class)
    val avgPrice: BigDecimal

    /**
     * The website property
     */
    val website: @NotBlank String?

    /**
     * The books property
     */
    @OneToMany(mappedBy = "store")
    val books: List<Book>

    /**
     * The `newestBooks` property
     */
    @Transient(BookStoreNewestBooksResolver::class)
    val newestBooks: List<Book>

    // For issue 714
    @Transient(BookStoreNewestBooksResolver::class)
    val newestBookIds: List<Long>
}