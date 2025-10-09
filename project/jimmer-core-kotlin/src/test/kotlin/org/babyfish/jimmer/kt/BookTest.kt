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
    fun `test hide and show should not affect base object`() {
        // Given: 创建一个 book 并隐藏 name 属性
        val book = Book {
            name = "book"
            edition = 1
            hide(this, BookDraft::name)
        }

        // When: 基于 book 创建 newBook 并显示 name 属性
        val newBook = Book(book) {
            name = "newBook"
            show(this, BookDraft::name)
        }

        // Then: 验证两个对象的状态
        // 1. book 的 name 应该保持隐藏状态
        assertEquals("book", book.name) // name 值应该保持不变
        // 验证 book 的 JSON 序列化不包含 name（如果可能的话）

        // 2. newBook 的 name 应该显示且值为新值
        assertEquals("newBook", newBook.name)

        // 3. 两个对象应该是不同的实例
        assertNotSame(book, newBook)

        // 4. 验证其他属性保持一致
        assertEquals(book.edition, newBook.edition)

        // 5. 验证两个对象的字符串表示不同
        assertNotEquals(book.toString(), newBook.toString())

        // 打印结果用于手动验证
        println("Original book: $book")
        println("New book: $newBook")
        println("Are they the same instance? ${book === newBook}")
    }

    @Test
    fun `test base object should remain unchanged after creating derived object`() {
        // Given: 创建一个初始对象
        val original = Book {
            name = "original"
            edition = 1
            hide(this, BookDraft::name)
        }

        val originalState = original.toString() // 此时 name 是隐藏的

        // When: 基于原始对象创建新对象并进行修改
        val modified = Book(original) {
            name = "modified"
            edition = 2
            show(this, BookDraft::name)
        }

        // Then: 验证序列化结果
        // original 应该保持 name 隐藏，modified 应该显示 name
        assertEquals("""{"edition":1}""", originalState) // name 被隐藏
        assertEquals("""{"name":"modified","edition":2}""", modified.toString()) // name 被显示

        // 但是程序访问时，两个对象都能正常访问 name 属性
        assertEquals("original", original.name)
        assertEquals("modified", modified.name)
    }

    @Test
    fun `test multiple derived objects should not interfere with each other`() {
        // Given: 创建一个基础对象
        val base = Book {
            name = "base"
            edition = 1
            hide(this, BookDraft::name)
        }

        // When: 创建两个不同的派生对象
        val derived1 = Book(base) {
            name = "derived1"
            show(this, BookDraft::name)
        }

        val derived2 = Book(base) {
            name = "derived2"
            // 不调用 show，name 应该保持隐藏
        }

        // Then: 所有对象应该保持各自的状态
        assertEquals("base", base.name)
        assertEquals(1, base.edition)

        assertEquals("derived1", derived1.name)
        assertEquals(1, derived1.edition)

        assertEquals("derived2", derived2.name)
        assertEquals(1, derived2.edition)

        // 基础对象应该保持不变
        val baseAfterOperations = Book {
            name = "base"
            edition = 1
            hide(this, BookDraft::name)
        }
        assertEquals(base.toString(), baseAfterOperations.toString())
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