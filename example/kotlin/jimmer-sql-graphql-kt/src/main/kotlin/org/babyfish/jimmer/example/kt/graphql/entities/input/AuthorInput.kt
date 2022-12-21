package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.ImmutableConverter
import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Gender
import org.babyfish.jimmer.kt.map
import org.babyfish.jimmer.spring.model.Input

class AuthorInput(
    val id: Long?, // Optional id
    val firstName: String,
    val lastName: String,
    val gender: Gender
) : Input<Author> {

    override fun toEntity(): Author =
        CONVERTER.convert(this)

    companion object {

        private val CONVERTER = ImmutableConverter
            .forFields(Author::class.java, AuthorInput::class.java)
            .map(Author::id) {
                useIf { it.id != null }
            }
            .build()
    }
}