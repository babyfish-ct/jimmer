package org.babyfish.jimmer.example.kt.graphql.input

import org.babyfish.jimmer.example.kt.graphql.entities.Book
import org.babyfish.jimmer.example.kt.graphql.entities.addBy
import org.babyfish.jimmer.example.kt.graphql.entities.by
import org.babyfish.jimmer.kt.new
import java.math.BigDecimal

data class BookInput(
    val id: Long?, // Optional id
    val name: String,
    val edition: Int,
    val price: BigDecimal,
    val storeId: Long?,
    val authorIds: List<Long>
) {

    fun toBook(): Book =
        new(Book::class).by {
            val that = this@BookInput
            that.id?.let {
                id = it
            }
            name = that.name
            edition = that.edition
            price = that.price
            that.storeId?.let {
                store().id = it
            }
            for (authorId in that.authorIds) {
                authors().addBy {
                    id = authorId
                }
            }
        }
}