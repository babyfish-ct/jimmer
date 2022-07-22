package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToMany

@Entity
interface Author {

    @Id
    val id: Long

    @Key
    val firstName: String

    @Key
    val lastName: String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>
}