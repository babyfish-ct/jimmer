package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.sql.*
import javax.validation.constraints.NotBlank


@Entity
interface Author {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        sequenceName = "AUTHOR_ID_SEQ"
    )
    val id: Long

    @Key
    val firstName: @NotBlank String

    @Key
    val lastName: @NotBlank String

    val gender: Gender

    @ManyToMany(mappedBy = "authors")
    val books: List<Book>
}