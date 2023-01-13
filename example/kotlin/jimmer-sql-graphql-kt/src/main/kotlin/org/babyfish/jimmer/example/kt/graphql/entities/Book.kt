package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.pojo.AutoScalarStrategy
import org.babyfish.jimmer.pojo.Static
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Entity
@StaticType(
    alias = "default",
    topLevelName = "BookInput",
    autoScalarStrategy = AutoScalarStrategy.DECLARED
)
interface Book : TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: @NotBlank String

    @Key
    val edition: @Positive Int

    val price: BigDecimal

    @ManyToOne
    @Static(name = "storeId", idOnly = true)
    val store: BookStore?

    @ManyToMany(orderedProps = [
        OrderedProp("firstName"),
        OrderedProp("lastName")
    ])
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumnName = "BOOK_ID",
        inverseJoinColumnName = "AUTHOR_ID"
    )
    @Static(name = "authorIds", idOnly = true)
    val authors: List<Author>
}