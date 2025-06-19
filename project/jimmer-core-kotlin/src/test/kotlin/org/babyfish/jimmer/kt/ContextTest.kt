package org.babyfish.jimmer.kt

import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.kt.model.Author
import org.babyfish.jimmer.kt.model.Book
import kotlin.test.Test
import kotlin.test.expect

class ContextTest {

    @Test
    fun testRequired() {
        val book = Book {
            name = "SQL in Action"
            val author1 = Author { firstName = "Jim" }
            val author2 = Author { firstName = "Jim" }
            expect(true) { author1 is Draft }
            expect(true) { author2 is Draft }
            authors = listOf(author1, author2)
        }
        expect("""{"name":"SQL in Action","authors":[{"firstName":"Jim"},{"firstName":"Jim"}]}""") {
            book.toString()
        }
    }

    @Test
    fun testRequiresNew() {
        val book = Book {
            name = "SQL in Action"
            val author1 = Author(true) { firstName = "Jim" }
            val author2 = Author(true) { firstName = "Jim" }
            expect(false) { author1 is Draft }
            expect(false) { author2 is Draft }
            authors = listOf(author1, author2)
        }
        expect("""{"name":"SQL in Action","authors":[{"firstName":"Jim"},{"firstName":"Jim"}]}""") {
            book.toString()
        }
    }
}