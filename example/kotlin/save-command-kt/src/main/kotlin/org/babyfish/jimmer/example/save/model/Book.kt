package org.babyfish.jimmer.example.save.model

import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity
interface Book {

    @Id //`identity(10, 10)` in database, so it is 10, 20, 30 ...
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    @Key
    val edition: Int

    val price: BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumnName = "BOOK_ID",
        inverseJoinColumnName = "AUTHOR_ID"
    )
    val authors: List<Author>
}