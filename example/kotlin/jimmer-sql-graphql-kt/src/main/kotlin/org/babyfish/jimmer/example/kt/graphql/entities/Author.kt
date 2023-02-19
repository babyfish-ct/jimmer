package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.example.kt.graphql.entities.common.BaseEntity
import org.babyfish.jimmer.sql.*
import javax.validation.constraints.NotBlank

@Entity
interface Author : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    val gender: Gender

    @ManyToMany(mappedBy = "authors", orderedProps = [
        OrderedProp("name"),
        OrderedProp("edition", desc = true)
    ])
    val books: List<Book>

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The simple calculated properties are shown here. As for the
    // complex calculated properties, you can view `BookStore.avgPrice` and
    // `BookStore.newestBooks`
    // -----------------------------

    @Formula(dependencies = ["firstName", "lastName"])
    val fullName: String
        get() = "$firstName $lastName"

    // The simple property above is simple calculation based on JAVA expression,
    // you can also define simple calculations given SQL expressions like this
    //
    // @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    // val fullName
}