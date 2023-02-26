package org.babyfish.jimmer.example.save.model

import org.babyfish.jimmer.sql.*

@Entity
interface Author {

    @Id // `identity(100, 100)` in database, so it is 100, 200, 300 ...
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val firstName: String

    @Key
    val lastName: String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>
}