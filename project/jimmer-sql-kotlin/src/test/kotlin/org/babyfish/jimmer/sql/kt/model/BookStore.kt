package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*

@Entity
interface BookStore {

    @Id
    val id: Long

    @Key
    val name: String

    @Version
    val version: Int

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}