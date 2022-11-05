package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.example.kt.graphql.entities.common.BaseEntity
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity
interface BookStore : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    val website: String?

    @Transient(BookStoreAvgPriceResolver::class)
    val avgPrice: BigDecimal

    @OneToMany(mappedBy = "store", orderedProps = [
        OrderedProp("name"),
        OrderedProp("edition", desc = true)
    ])
    val books: List<Book>
}