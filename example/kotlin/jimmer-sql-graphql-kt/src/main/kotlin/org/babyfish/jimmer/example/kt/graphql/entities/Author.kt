package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.example.kt.graphql.entities.common.CommonEntity
import org.babyfish.jimmer.sql.*
import javax.validation.constraints.NotBlank


@Entity
interface Author : CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    val gender: Gender

    @ManyToMany(mappedBy = "authors", orderedProps = [
        OrderedProp("name"),
        OrderedProp("edition", desc = true)
    ])
    val books: List<Book>
}