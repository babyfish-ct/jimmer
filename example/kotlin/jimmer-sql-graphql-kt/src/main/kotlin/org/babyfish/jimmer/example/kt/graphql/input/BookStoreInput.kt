package org.babyfish.jimmer.example.kt.graphql.input

import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.example.kt.graphql.entities.by
import org.babyfish.jimmer.kt.new

class BookStoreInput(
    val id: Long?, // Optional id
    val name: String,
    val website: String?
) {

    fun toBookStore(): BookStore =
        new(BookStore::class).by {
            val that = this@BookStoreInput
            that.id?.let {
                id = it
            }
            name = that.name
            website = that.website
        }
}