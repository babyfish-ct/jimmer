package org.babyfish.jimmer.example.save.model

import org.babyfish.jimmer.sql.*

@Entity
interface BookStore {

    @Id // `identity(1, 1)` in database, so it is 1, 2, 3 ...
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: String

    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}