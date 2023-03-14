package org.babyfish.jimmer.sql.kt.model.classic.author

import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.Organization
import javax.validation.constraints.NotBlank

@Entity
interface Author {

    @Id
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    @Formula(dependencies = ["firstName", "lastName"])
    val fullName: String
        get() = "$firstName $lastName"

    @Formula(sql = "concat(%alias.FIRST_NAME, ' ', %alias.LAST_NAME)")
    val fullName2: String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>

    @Transient
    val organization: Organization

    fun unusedFun() {}
}