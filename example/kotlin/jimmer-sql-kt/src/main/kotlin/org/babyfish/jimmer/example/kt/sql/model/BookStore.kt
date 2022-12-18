package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.example.kt.sql.dal.calc.BookStoreAvgPriceResolver
import org.babyfish.jimmer.example.kt.sql.model.common.BaseEntity
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

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}