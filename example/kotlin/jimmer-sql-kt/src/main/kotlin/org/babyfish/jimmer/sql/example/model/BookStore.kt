package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.model.calc.BookStoreAvgPriceResolver
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

    @Transient(BookStoreAvgPriceResolver::class)
    val avgPrice: BigDecimal

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}