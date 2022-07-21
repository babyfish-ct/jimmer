package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.Key
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Version

@Entity
interface BookStore {

    @get:Id
    val id: Long

    @get:Key
    val name: String

    @get:Version
    val version: Int

    @get:OneToMany(mappedBy = "store")
    val books: List<Book>
}