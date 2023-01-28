package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.example.kt.sql.model.common.TenantAware
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity
@StaticType(
    alias = "input",
    topLevelName = "BookInput",
    autoScalarStrategy = AutoScalarStrategy.DECLARED
)
@StaticType(
    alias = "compositeInput",
    topLevelName = "CompositeBookInput",
    autoScalarStrategy = AutoScalarStrategy.DECLARED
)
interface Book : TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    @Key
    val edition: Int
    val price: BigDecimal

    @ManyToOne
    @Static(name = "storeId", idOnly = true)
    val store: BookStore?

    @ManyToMany
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumnName = "BOOK_ID",
        inverseJoinColumnName = "AUTHOR_ID"
    )
    @Static(name = "authorIds", idOnly = true)
    val authors: List<Author>

    @OneToMany(mappedBy = "book", orderedProps = [OrderedProp("index")])

    @Static(alias = "compositeInput")
    val chapters: List<Chapter>
}