package org.babyfish.jimmer.sql.example.model

import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.example.model.common.BaseEntity

@Entity
interface Author : BaseEntity {

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