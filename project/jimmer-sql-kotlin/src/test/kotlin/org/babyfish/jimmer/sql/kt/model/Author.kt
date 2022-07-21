package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Key
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.ManyToMany

@Entity
interface Author {

    @get:Id
    val id: Long

    @get:Key
    val firstName: String

    @get:Key
    val lastName: String

    val gender: Gender

    @get:ManyToMany(mappedBy = "authors")
    val books: List<Book>
}