package org.babyfish.jimmer.example.kt.graphql.entities.input

import org.babyfish.jimmer.example.kt.graphql.entities.BookStore
import org.babyfish.jimmer.kt.newImmutableConverter

class BookStoreInput(
    val id: Long?, // Optional id
    val name: String,
    val website: String?
) {

    fun toBookStore(): BookStore =
        BOOK_STORE_CONVERTER.convert(this)

    companion object {

        private val BOOK_STORE_CONVERTER =
            newImmutableConverter(BookStore::class, BookStoreInput::class) {
                map(BookStore::id) {
                    useIf { it.id != null }
                }
                autoMapOtherScalars(true)
            }
    }
}