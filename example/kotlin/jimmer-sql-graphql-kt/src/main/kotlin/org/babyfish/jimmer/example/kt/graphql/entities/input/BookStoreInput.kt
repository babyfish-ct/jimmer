package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.ImmutableConverter
import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.kt.map
import org.babyfish.jimmer.spring.model.Input

class BookStoreInput(
    val id: Long?, // Optional id
    val name: String,
    val website: String?
): Input<BookStore> {

    override fun toEntity(): BookStore =
        CONVERTER.convert(this)

    companion object {

        private val CONVERTER = ImmutableConverter
            .forFields(BookStore::class.java, BookStoreInput::class.java)
            .map(BookStore::id) {
                useIf { it.id != null }
            }
            .build()
    }
}