package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Gender
import org.babyfish.jimmer.kt.newImmutableConverter

class AuthorInput(
    val id: Long?, // Optional id
    val firstName: String,
    val lastName: String,
    val gender: Gender
) {

    fun toAuthor(): Author =
        AUTHOR_CONVERTER.convert(this)

    companion object {

        private val AUTHOR_CONVERTER =
            newImmutableConverter(Author::class, AuthorInput::class) {
                map(Author::id) {
                    useIf { it.id != null }
                }
                autoMapOtherScalars(true)
            }
    }
}