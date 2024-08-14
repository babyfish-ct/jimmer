package org.babyfish.jimmer.kt

import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.kt.model.Book
import org.babyfish.jimmer.kt.model.addBy
import org.babyfish.jimmer.kt.model.by
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.expect

class BookTest {

    @Test
    fun test() {
        val book = Book {
            name = "book"
            createdTime = LocalDateTime.of(2024, 8, 15, 4, 27, 23)
            store().name = "store"
            authors().addBy {
                firstName = "Jim"
            }
            authors().addBy {
                firstName = "Kate"
            }
        }

        val book2 = ImmutableObjects.fromString(Book::class.java, book.toString())

        val book3 = Book(book) {
            name += "@"
            store().name += "#"
            for (author in authors()) {
                author.firstName += "!"
            }
        }

        val json =
            """{
                |--->"name":"book",
                |--->"createdTime":"2024-08-15 04:27:23",
                |--->"store":{
                |--->--->"name":"store"
                |--->},
                |--->"authors":[
                |--->--->{"firstName":"Jim"},
                |--->--->{"firstName":"Kate"}
                |--->]
                |}""".trimMargin().toSimpleJson()

        val newJson =
            """{
                |--->"name":"book@",
                |--->"createdTime":"2024-08-15 04:27:23",
                |--->"store":{
                |--->--->"name":"store#"
                |--->},
                |--->"authors":[
                |--->--->{"firstName":"Jim!"},
                |--->--->{"firstName":"Kate!"}
                |--->]
                |}""".trimMargin().toSimpleJson()

        expect(json) { book.toString() }
        expect(json) { book2.toString() }
        expect(newJson) { book3.toString() }
    }
}