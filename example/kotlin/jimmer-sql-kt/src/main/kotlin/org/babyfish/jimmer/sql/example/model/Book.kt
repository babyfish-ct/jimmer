package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.model.common.BaseEntity
import org.babyfish.jimmer.sql.example.model.common.TenantAware
import java.math.BigDecimal

@Entity
interface Book : BaseEntity, TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key // ❶
    val name: String

    @Key // ❷
    val edition: Int
    val price: BigDecimal

    @ManyToOne // ❸
    val store: BookStore?

    @ManyToMany // ❹
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumnName = "BOOK_ID",
        inverseJoinColumnName = "AUTHOR_ID"
    )
    val authors: List<Author>

    // -----------------------------
    // Optional properties
    // -----------------------------

    // Optional property `storeId`
    // If this property is deleted, please add `BookInput.Mapper.toBookStore(Long)`
    @IdView // ❺
    val storeId: Long?

    // Optional property `authorIds`
    // If this property is deleted, please add `BookInputMapper.toAuthor(Long)`
    @IdView("authors") // ❻
    val authorIds: List<Long>
}

/*----------------Documentation Links----------------
❶ ❷ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-one
❹ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-many
❺ ❻ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/view/id-view
---------------------------------------------------*/
