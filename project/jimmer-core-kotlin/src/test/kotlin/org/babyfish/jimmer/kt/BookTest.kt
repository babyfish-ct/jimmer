package org.babyfish.jimmer.kt

import junit.framework.TestCase.assertEquals
import org.babyfish.jimmer.ImmutableObjects
import org.babyfish.jimmer.kt.model.Book
import org.babyfish.jimmer.kt.model.BookDraft
import org.babyfish.jimmer.kt.model.addBy
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.expect

class BookTest {

    @Test
    fun `test base object should remain unchanged after creating derived object`() {
        val original = Book {
            name = "original"
            edition = 1
            hide(this, BookDraft::name)
        }

        val originalState = original.toString()

        val modified = Book(original) {
            name = "modified"
            edition = 2
            show(this, BookDraft::name)
        }

        assertEquals("""{"edition":1}""", originalState)
        assertEquals("""{"name":"modified","edition":2}""", modified.toString())

        assertEquals("original", original.name)
        assertEquals("modified", modified.name)
    }

    @Test
    fun `test multiple derived objects should not interfere with each other`() {
        val base = Book {
            name = "base"
            edition = 1
            hide(this, BookDraft::name)
        }

        val baseState = base.toString()

        val derived1 = Book(base) {
            name = "derived1"
            show(this, BookDraft::name)
        }

        val derived2 = Book(base) {
            name = "derived2"
        }

        assertEquals("""{"edition":1}""", baseState)

        assertEquals("""{"name":"derived1","edition":1}""", derived1.toString())
        assertEquals("derived1", derived1.name)

        assertEquals("""{"edition":1}""", derived2.toString())
        assertEquals("derived2", derived2.name)

        assertEquals("base", base.name)
        assertEquals(1, base.edition)
    }

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