package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.calc.BookStoreAvgPriceResolver
import org.babyfish.jimmer.sql.kt.model.calc.BookStoreNewestBooksResolver
import java.math.BigDecimal
import javax.validation.constraints.NotBlank

@Entity
interface BookStore {

    @Id
    val id: Long

    @Key
    val name: @NotBlank String

    @Version
    val version: Int

    @Transient(BookStoreAvgPriceResolver::class)
    val avgPrice: BigDecimal

    val website: @NotBlank String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>

    @Transient(BookStoreNewestBooksResolver::class)
    val newestBook: List<Book>
}