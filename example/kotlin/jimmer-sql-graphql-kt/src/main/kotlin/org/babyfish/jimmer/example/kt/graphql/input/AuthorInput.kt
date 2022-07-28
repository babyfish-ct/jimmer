package org.babyfish.jimmer.example.kt.graphql.input

import org.babyfish.jimmer.example.kt.graphql.entities.Author
import org.babyfish.jimmer.example.kt.graphql.entities.Gender
import org.babyfish.jimmer.example.kt.graphql.entities.by
import org.babyfish.jimmer.kt.new

class AuthorInput(
    val id: Long?, // Optional id
    val firstName: String,
    val lastName: String,
    val gender: Gender
) {

    fun toAuthor(): Author =
        new(Author::class).by {
            val that = this@AuthorInput
            that.id?.let {
                id = it
            }
            firstName = that.firstName
            lastName = that.lastName
            gender = that.gender
        }
}