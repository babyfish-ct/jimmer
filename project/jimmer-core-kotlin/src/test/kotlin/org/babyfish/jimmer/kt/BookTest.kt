package org.babyfish.jimmer.kt

import org.babyfish.jimmer.kt.model.Book
import org.babyfish.jimmer.kt.model.addBy
import org.babyfish.jimmer.kt.model.by
import kotlin.test.Test
import kotlin.test.expect

class BookTest {

    @Test
    fun test() {
        val book = new(Book::class).by {
            name = "book"
            store().name = "store"
            authors().addBy {
                firstName = "Jim"
            }
            authors().addBy {
                lastName = "Kate"
            }
        }
        expect(
            """{
                |--->"name":"book",
                |--->"store":{
                |--->--->"name":"store"
                |--->},
                |--->"authors":[
                |--->--->{"firstName":"Jim"},
                |--->--->{"lastName":"Kate"}
                |--->]
                |}""".trimMargin().toSimpleJson()
        ) { book.toString() }
    }
}