package org.babyfish.jimmer.example.kt.sql.model.input

import org.babyfish.jimmer.ImmutableConverter
import org.babyfish.jimmer.example.kt.sql.model.Book
import org.babyfish.jimmer.kt.makeIdOnly
import org.babyfish.jimmer.kt.map
import org.babyfish.jimmer.kt.mapList
import org.babyfish.jimmer.spring.java.model.Input
import java.math.BigDecimal

data class BookInput(
    val id: Long?,
    val name: String,
    val edition: Int,
    val price: BigDecimal,
    val storeId: Long?,
    val authorIds: List<Long>
): Input<Book> {

    override fun toEntity(): Book =
        CONVERTER.convert(this)

    companion object {

        @JvmStatic
        private val CONVERTER = ImmutableConverter
            .forFields(Book::class.java, BookInput::class.java)
            .map(Book::id) {
                useIf { it.id !== null }
            }
            .map(Book::store, BookInput::storeId) {
                valueConverter(::makeIdOnly)
            }
            .mapList(Book::authors, BookInput::authorIds) {
                elementConverter(::makeIdOnly)
            }
            .build()
    }
}