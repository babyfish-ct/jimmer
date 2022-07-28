package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator
import java.math.BigDecimal
import java.util.*

@Entity
interface Book {

    @Id
    @GeneratedValue(strategy = GenerationType.USER, generatorType = UUIDIdGenerator::class)
    val id: UUID

    @Key
    val name: String

    @Key
    val edition: Int
    val price: BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    @JoinTable(name = "BOOK_AUTHOR_MAPPING", joinColumnName = "BOOK_ID", inverseJoinColumnName = "AUTHOR_ID")
    val authors: List<Author>
}