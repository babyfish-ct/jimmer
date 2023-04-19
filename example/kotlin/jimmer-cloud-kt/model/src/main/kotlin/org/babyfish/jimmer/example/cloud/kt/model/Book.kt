package org.babyfish.jimmer.example.cloud.kt.model

import org.babyfish.jimmer.example.cloud.kt.model.common.BaseEntity
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity(microServiceName = "book-service")
interface Book : BaseEntity {

    @Id
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