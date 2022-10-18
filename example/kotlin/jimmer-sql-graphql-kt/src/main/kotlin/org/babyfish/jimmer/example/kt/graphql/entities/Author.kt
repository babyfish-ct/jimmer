package org.babyfish.jimmer.example.kt.graphql.entities

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

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>
}