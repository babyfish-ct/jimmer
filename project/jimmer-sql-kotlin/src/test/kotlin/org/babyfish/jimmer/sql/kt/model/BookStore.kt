package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import javax.validation.constraints.Null

@Entity
interface BookStore {

    @Id
    val id: Long

    @Key
    val name: String

    @Version
    val version: Int

    val website: String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}