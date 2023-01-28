package org.babyfish.jimmer.example.kt.sql.model

import org.babyfish.jimmer.example.kt.sql.model.common.BaseEntity
import org.babyfish.jimmer.pojo.StaticType
import org.babyfish.jimmer.sql.*

@Entity
@StaticType(alias = "input", topLevelName = "AuthorInput")
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