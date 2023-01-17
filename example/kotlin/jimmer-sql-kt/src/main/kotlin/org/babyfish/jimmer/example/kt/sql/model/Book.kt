package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.example.kt.sql.model.common.TenantAware
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal

@Entity
@StaticType(
    alias = "default",
    topLevelName = "BookInput",
    autoScalarStrategy = AutoScalarStrategy.DECLARED
)
@StaticType(
    alias = "composite",
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

    /*
     * Here, `chapters` is mapped to static type, the `targetAlias` in `declaredOnly`.
     *
     * The target type `Chapter` is decorated by the annotation @StaticType, whose `alias`
     * is `declaredOnly` and `autoScalarStrategy` is `AutoScalarStrategy.DECLARED`. That means
     * only the properties declared in `Chapter` should be mapped automatically, not include
     * the properties inherited from `BaseEntity`.
     *
     * There is another solution: using `@Static(enabled = false)` to decorate properties
     * of `BaseEntity`, let those properties cannot be mapped into static types. That means
     * it is unnecessary to specify the static type of `Chapter` with `AutoScalarStrategy.DECLARED`,
     * at this time, the `targetAlias` of the current property can be unspecified.
     */
    @Static(alias = "composite", targetAlias = "declaredOnly")
    val chapters: List<Chapter>
}