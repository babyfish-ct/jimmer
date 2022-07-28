package org.babyfish.jimmer.sql.kt.model

import org.babyfish.jimmer.sql.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Null

@Entity
interface BookStore {

    @Id
    val id: Long

    @Key
    val name: @NotBlank String

    @Version
    val version: Int

    val website: @NotBlank String?

    @OneToMany(mappedBy = "store")
    val books: List<Book>
}