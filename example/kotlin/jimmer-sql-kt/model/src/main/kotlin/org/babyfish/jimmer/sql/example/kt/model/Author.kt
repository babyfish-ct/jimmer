package org.babyfish.jimmer.sql.example.kt.model

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.kt.model.common.BaseEntity


@Entity
interface Author : BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    // It is inappropriate to use `firstName` and `lastName`
    // as keys in actual project, but this is just a small demo.

    @Key // ❶
    val firstName: String

    @Key // ❷
    val lastName: String

    val gender: Gender

    @ManyToMany(mappedBy = "authors") // ❸
    val books: List<Book>

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The simple calculated properties are shown here. As for the
    // complex calculated properties, you can view `BookStore.avgPrice` and
    // `BookStore.newestBooks`
    // -----------------------------

    // -----------------------------
    //
    // Everything below this line are calculated properties.
    //
    // The simple calculated properties are shown here. As for the
    // complex calculated properties, you can view `BookStore.avgPrice` and
    // `BookStore.newestBooks`
    // -----------------------------
    @Formula(dependencies = ["firstName", "lastName"]) // ❹
    val fullName: String
        get() = "$firstName $lastName"

    // The simple property above is simple calculation based on JAVA expression,
    // you can also define simple calculations given SQL expressions like this
    //
    // @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    // val fullName
}

/*----------------Documentation Links----------------
❶ ❷ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/key
❸ https://babyfish-ct.github.io/jimmer/docs/mapping/base/association/many-to-many
❹ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/calculated/formula
---------------------------------------------------*/
