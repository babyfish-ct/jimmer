package org.babyfish.jimmer.example.cloud.kt.model

import org.babyfish.jimmer.sql.*

@Entity(microServiceName = "author-service")
interface Author {

    @Id
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