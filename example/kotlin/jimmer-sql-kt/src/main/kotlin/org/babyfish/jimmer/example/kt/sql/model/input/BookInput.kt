package org.babyfish.jimmer.example.kt.sql.model.input

import org.babyfish.jimmer.ImmutableConverter
import org.babyfish.jimmer.example.kt.sql.model.Author
import org.babyfish.jimmer.example.kt.sql.model.Book
import org.babyfish.jimmer.example.kt.sql.model.BookStore
import org.babyfish.jimmer.kt.makeIdOnly
import org.babyfish.jimmer.kt.newImmutableConverter
import java.math.BigDecimal

data class BookInput(
    val id: Long?,
    val name: String,
    val edition: Int,
    val price: BigDecimal,
    val storeId: Long?,
    val authorIds: List<Long>
) {
    fun toBook(): Book =
        BOOK_CONVERTER.convert(this)

    companion object {

        @JvmStatic
        private val BOOK_CONVERTER = newImmutableConverter(Book::class, BookInput::class) {
            map(Book::id) {
                useIf { it.id !== null }
            }
            map(Book::store, BookInput::storeId) {
                valueConverter(::makeIdOnly)
            }
            mapList(Book::authors, BookInput::authorIds) {
                elementConverter(::makeIdOnly)
            }
            autoMapOtherScalars()
        }
    }
}