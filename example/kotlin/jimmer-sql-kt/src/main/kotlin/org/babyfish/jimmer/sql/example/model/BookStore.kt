package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.bll.resolver.BookStoreAvgPriceResolver
import org.babyfish.jimmer.sql.example.bll.resolver.BookStoreNewestBooksResolver
import org.babyfish.jimmer.sql.example.model.common.BaseEntity
import java.math.BigDecimal

@Entity
interface BookStore : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String
    
    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The complex calculated properties are shown here.
    // As for the simple calculated properties, you can view `Author.fullName`
    // -----------------------------

    @Transient(BookStoreAvgPriceResolver::class)
    val avgPrice: BigDecimal

    /*
     * For example, if `BookStore.books` returns `[
     *     {name: A, edition: 1}, {name: A, edition: 2}, {name: A, edition: 3},
     *     {name: B, edition: 1}, {name: B, edition: 2}
     * ]`, `BookStore.newestBooks` returns `[
     *     {name: A, edition: 3}, {name: B, edition: 2}
     * ]`
     *
     * It is worth noting that if the calculated property returns entity object
     * or entity list, the shape can be controlled by the deeper child fetcher
     */
    @Transient(BookStoreNewestBooksResolver::class)
    val newestBooks: List<Book>
}