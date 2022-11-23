package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import javax.validation.constraints.NotBlank

@Entity
interface Author {

    @Id
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    val fullName: String
        get() = "$firstName $lastName"

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>

    @Transient
    val organization: Organization

    fun unusedFun() {}
}