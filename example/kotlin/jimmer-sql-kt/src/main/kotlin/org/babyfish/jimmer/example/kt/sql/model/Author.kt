package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator
import java.util.*

@Entity
interface Author {

    @Id
    @GeneratedValue(
        strategy = GenerationType.USER,
        generatorType = UUIDIdGenerator::class
    )
    val id: UUID

    @Key
    val firstName: String

    @Key
    val lastName: String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>
}