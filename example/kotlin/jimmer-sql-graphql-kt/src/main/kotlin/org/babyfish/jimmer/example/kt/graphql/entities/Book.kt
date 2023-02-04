package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.example.kt.graphql.entities.common.TenantAware
import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Entity
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
    val authors: List<Author>

    @OneToMany(mappedBy = "book", orderedProps = [OrderedProp("index")])
    val chapters: List<Chapter>
}